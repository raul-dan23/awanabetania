package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Model.Score;
import com.awanabetania.awanabetania.Model.ScoreRequest;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.MeetingRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import com.awanabetania.awanabetania.Repository.ScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Handles scoring for children at each club meeting.
 * A score is recorded once per child per active meeting; duplicate submissions are rejected.
 * Points are accumulated into both {@code seasonPoints} (cumulative) and {@code dailyPoints}
 * (reset on meeting close). Attendance milestones trigger director notifications.
 *
 * <p>Point values per criterion:
 * <ul>
 *   <li>Attended: 1 000</li>
 *   <li>Bible: 500</li>
 *   <li>Handbook: 500</li>
 *   <li>Lesson: 1 000</li>
 *   <li>Friend: 1 000</li>
 *   <li>Uniform: 10 000</li>
 *   <li>Extra: as specified</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/scores")
@CrossOrigin(origins = "*")
public class ScoreController {

    @Autowired private ScoreRepository scoreRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private NotificationRepository notificationRepository;

    /**
     * Records a score for a child at the current active meeting.
     * Finds the earliest open meeting, checks for duplicate entries, computes the point total,
     * updates the child's season and daily point counters, and triggers milestone notifications.
     *
     * @param request scoring form data (child ID and per-criterion booleans)
     * @return 200 with the total points on success; 400 if no active session, unknown child, or duplicate
     */
    @PostMapping("/add")
    public ResponseEntity<?> addScore(@RequestBody ScoreRequest request) {
        Meeting meeting = meetingRepository.findByIsCompletedFalseOrderByDateAsc()
                .stream().findFirst().orElse(null);
        if (meeting == null) return ResponseEntity.badRequest().body("No active session exists.");

        Child child = childRepository.findById(request.getChildId()).orElse(null);
        if (child == null) return ResponseEntity.badRequest().body("Invalid child.");

        boolean alreadyScored = scoreRepository.findByChildIdAndMeetingId(child.getId(), meeting.getId()).isPresent();
        if (alreadyScored) return ResponseEntity.badRequest().body("This child has already been scored today.");

        Score score = new Score();
        score.setChild(child);
        score.setMeeting(meeting);
        score.setDate(LocalDate.now());
        score.setAttended(Boolean.TRUE.equals(request.getAttended()));
        score.setHasBible(Boolean.TRUE.equals(request.getHasBible()));
        score.setHasHandbook(Boolean.TRUE.equals(request.getHasHandbook()));
        score.setLesson(Boolean.TRUE.equals(request.getLesson()));
        score.setFriend(Boolean.TRUE.equals(request.getFriend()));
        score.setHasUniform(Boolean.TRUE.equals(request.getHasUniform()));
        score.setExtraPoints(request.getExtraPoints() != null ? request.getExtraPoints() : 0);

        int points = calculatePoints(score);
        score.setIndividualPoints(points);
        score.setTotal(points);
        score.setDetails(generateDetailsString(score));

        child.setSeasonPoints((child.getSeasonPoints() == null ? 0 : child.getSeasonPoints()) + points);
        child.setDailyPoints((child.getDailyPoints() == null ? 0 : child.getDailyPoints()) + points);

        if (Boolean.TRUE.equals(request.getAttended())) {
            int currentStreak = (child.getAttendanceStreak() == null) ? 0 : child.getAttendanceStreak();
            int newStreak = currentStreak + 1;
            child.setAttendanceStreak(newStreak);

            // Use the meeting's date (not today) so streak logic stays correct if the director
            // closes the meeting on a different calendar day than it occurred.
            child.setLastAttendanceDate(meeting.getDate());

            child.setTotalAttendance((child.getTotalAttendance() == null ? 0 : child.getTotalAttendance()) + 1);
            if (Boolean.TRUE.equals(request.getLesson())) {
                child.setLessonsCompleted((child.getLessonsCompleted() == null ? 0 : child.getLessonsCompleted()) + 1);
            }

            checkRewards(child, newStreak);
        }

        scoreRepository.save(score);
        childRepository.save(child);

        return ResponseEntity.ok("Points saved. Total: " + points);
    }

    /**
     * Returns the full scoring history for a child, newest meeting first.
     *
     * @param childId the child's primary key
     * @return list of {@link Score} records
     */
    @GetMapping("/child/{childId}")
    public List<Score> getScoresByChild(@PathVariable Integer childId) {
        return scoreRepository.findByChildIdOrderByMeeting_DateDesc(childId);
    }

    /**
     * Creates reward notifications for the director at attendance milestones (5 and 10 meetings).
     * Skips notification creation if the reward has already been given or if a notification
     * for this reward type is already visible.
     */
    private void checkRewards(Child child, int streak) {
        if (streak == 5 && (child.getHasShirt() == null || !child.getHasShirt())) {
            createNotification(child, "SHIRT_ELIGIBLE",
                    "DIRECTOR! " + child.getName() + " is eligible for their SHIRT (5 attendances).");
        }
        if (streak == 10 && (child.getHasHat() == null || !child.getHasHat())) {
            createNotification(child, "HAT_ELIGIBLE",
                    "DIRECTOR! " + child.getName() + " is eligible for their HAT (10 attendances).");
        }
    }

    /** Creates a director notification only if no active notification of the same type exists for the child. */
    private void createNotification(Child child, String type, String msg) {
        List<Notification> existing = notificationRepository.findActiveByChildAndType(child.getId(), type);
        if (existing.isEmpty()) {
            Notification n = new Notification();
            n.setMessage(msg);
            n.setType(type);
            n.setVisibleTo("DIRECTOR");
            n.setDate(LocalDate.now());
            n.setIsVisible(true);
            n.setChildId(child.getId());
            notificationRepository.save(n);
        }
    }

    /** Calculates the total points for a score based on which criteria were fulfilled. */
    private int calculatePoints(Score s) {
        int total = 0;
        if (Boolean.TRUE.equals(s.getAttended())) total += 1000;
        if (Boolean.TRUE.equals(s.getHasBible())) total += 500;
        if (Boolean.TRUE.equals(s.getHasHandbook())) total += 500;
        if (Boolean.TRUE.equals(s.getLesson())) total += 1000;
        if (Boolean.TRUE.equals(s.getFriend())) total += 1000;
        if (Boolean.TRUE.equals(s.getHasUniform())) total += 10000;
        if (s.getExtraPoints() != null) total += s.getExtraPoints();
        return total;
    }

    /** Builds the comma-separated human-readable details string for a score. */
    private String generateDetailsString(Score s) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(s.getAttended())) sb.append("Prezenta, ");
        if (Boolean.TRUE.equals(s.getHasBible())) sb.append("Biblie, ");
        if (Boolean.TRUE.equals(s.getHasHandbook())) sb.append("Manual, ");
        if (Boolean.TRUE.equals(s.getLesson())) sb.append("Lectie, ");
        if (Boolean.TRUE.equals(s.getFriend())) sb.append("Prieten, ");
        if (Boolean.TRUE.equals(s.getHasUniform())) sb.append("Uniforma, ");
        if (s.getExtraPoints() != null && s.getExtraPoints() > 0) sb.append("Extra (+").append(s.getExtraPoints()).append("), ");
        return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "Points awarded";
    }
}
