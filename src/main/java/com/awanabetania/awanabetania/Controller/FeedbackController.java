package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Handles end-of-evening feedback: a general meeting rating plus per-leader evaluations.
 * Evaluations use soft deletion — setting {@code isVisible=false} hides them from
 * the UI without removing the data from the database. The leader's average rating
 * is recalculated whenever an evaluation is saved or soft-deleted.
 */
@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired private MeetingRepository meetingRepository;
    @Autowired private LeaderEvaluationRepository evaluationRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private NotificationRepository notificationRepository;

    /**
     * Returns the general rating and all visible individual evaluations for a given meeting.
     *
     * @param meetingId the meeting's primary key
     * @return map with "generalRating", "generalFeedback", and "evaluations"; 400 if not found
     */
    @GetMapping("/{meetingId}")
    public ResponseEntity<?> getFeedback(@PathVariable Integer meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null) return ResponseEntity.badRequest().build();

        List<LeaderEvaluation> evals = evaluationRepository.findByDateAndIsVisibleTrue(meeting.getDate());

        return ResponseEntity.ok(Map.of(
                "generalRating", meeting.getGeneralRating() != null ? meeting.getGeneralRating() : 0,
                "generalFeedback", meeting.getGeneralFeedback() != null ? meeting.getGeneralFeedback() : "",
                "evaluations", evals
        ));
    }

    /**
     * Returns the visible evaluation history for a given leader, newest first.
     *
     * @param leaderId the leader's primary key
     * @return list of visible {@link LeaderEvaluation} records
     */
    @GetMapping("/leader/{leaderId}")
    public List<LeaderEvaluation> getLeaderHistory(@PathVariable Integer leaderId) {
        return evaluationRepository.findByLeaderIdAndIsVisibleTrueOrderByDateDesc(leaderId);
    }

    /**
     * Soft-deletes an evaluation by setting {@code isVisible=false}.
     * Recalculates the affected leader's average rating to exclude the hidden entry.
     *
     * @param id the evaluation's primary key
     * @return 200 on success (no-op if the ID is not found)
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteEvaluation(@PathVariable Integer id) {
        LeaderEvaluation eval = evaluationRepository.findById(id).orElse(null);
        if (eval != null) {
            eval.setIsVisible(false);
            evaluationRepository.save(eval);
            recalculateLeaderRating(eval.getLeader().getId());
        }
        return ResponseEntity.ok("Evaluation hidden.");
    }

    /**
     * Saves the full feedback report for a meeting: general rating + individual evaluations.
     * Each evaluation triggers a notification to the evaluated leader and a rating recalculation.
     *
     * @param payload JSON with "meetingId", "directorId", "generalRating", "generalFeedback",
     *                and "evaluations" (list of {leaderId, rating, comment})
     * @return 200 on success; 400 if the meeting is not found
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveFeedback(@RequestBody Map<String, Object> payload) {
        Integer meetingId = (Integer) payload.get("meetingId");
        Integer directorId = (Integer) payload.get("directorId");

        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null) return ResponseEntity.badRequest().body("Meeting not found.");

        meeting.setGeneralRating((Integer) payload.get("generalRating"));
        meeting.setGeneralFeedback((String) payload.get("generalFeedback"));
        meetingRepository.save(meeting);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> evals = (List<Map<String, Object>>) payload.get("evaluations");

        for (Map<String, Object> evalData : evals) {
            Integer leaderId = (Integer) evalData.get("leaderId");
            Integer rating = (Integer) evalData.get("rating");
            String comment = (String) evalData.get("comment");

            Leader l = leaderRepository.findById(leaderId).orElse(null);
            if (l != null) {
                LeaderEvaluation le = new LeaderEvaluation();
                le.setDate(meeting.getDate());
                le.setEvaluatedBy(directorId);
                le.setRating(rating);
                le.setComment(comment);
                le.setIsVisible(true);
                le.setLeader(l);
                evaluationRepository.save(le);
                recalculateLeaderRating(leaderId);

                String notifMessage = String.format("%s\nRating: %d stars\nFeedback: %s",
                        meeting.getDate(), rating, comment);
                notificationRepository.save(
                        new Notification(notifMessage, "FEEDBACK", String.valueOf(leaderId), LocalDate.now()));
            }
        }

        return ResponseEntity.ok("Feedback saved.");
    }

    /**
     * Recomputes the leader's average {@code rating} from all currently visible evaluations.
     * Sets the rating to 0.0 if there are no visible evaluations.
     * The result is rounded to one decimal place.
     */
    private void recalculateLeaderRating(Integer leaderId) {
        Leader leader = leaderRepository.findById(leaderId).orElse(null);
        if (leader != null) {
            List<LeaderEvaluation> visibleEvals =
                    evaluationRepository.findByLeaderIdAndIsVisibleTrueOrderByDateDesc(leaderId);
            if (!visibleEvals.isEmpty()) {
                double average = visibleEvals.stream().mapToInt(LeaderEvaluation::getRating).average().orElse(0.0);
                float roundedAvg = (float) (Math.round(average * 10.0) / 10.0);
                leader.setRating(roundedAvg);
            } else {
                leader.setRating(0.0f);
            }
            leaderRepository.save(leader);
        }
    }
}
