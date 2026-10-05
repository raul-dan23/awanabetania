package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Model.Score;
import com.awanabetania.awanabetania.Model.Warning;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.MeetingRepository;
import com.awanabetania.awanabetania.Repository.ScoreRepository;
import com.awanabetania.awanabetania.Repository.WarningRepository;
import com.awanabetania.awanabetania.Service.SeasonService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages club meetings: listing active sessions, PIN verification, and closing a meeting.
 *
 * <p>Closing a meeting triggers four side effects:
 * <ol>
 *   <li>The meeting is marked completed.</li>
 *   <li>Suspension counters are decremented for children who were present;
 *       suspensions that reach 0 remaining meetings are lifted automatically.</li>
 *   <li>All children's {@code dailyPoints} and {@code currentTeam} fields are reset.</li>
 *   <li>Children who were absent have their attendance streak reset to 0.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    @Autowired private MeetingRepository meetingRepository;
    @Autowired private WarningRepository warningRepository;
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private SeasonService seasonService;

    /**
     * Returns all meetings that have not yet been closed, ordered by date ascending.
     *
     * @return list of active (incomplete) meetings
     */
    @GetMapping
    public List<Meeting> getUpcomingMeetings() {
        return meetingRepository.findByIsCompletedFalseOrderByDateAsc();
    }

    /**
     * Creates a new meeting.
     *
     * @param meeting the meeting data from the request body
     * @return the saved {@link Meeting} entity with its generated ID
     */
    @PostMapping("/add")
    public Meeting addMeeting(@RequestBody Meeting meeting) {
        meeting.setSeasonId(seasonService.currentId());
        return meetingRepository.save(meeting);
    }

    /**
     * Verifies the 4-digit PIN for the currently active meeting.
     * The PIN is generated when a secretariat leader is first assigned to the meeting.
     *
     * @param payload JSON body with key "pin"
     * @return 200 "Correct" on match; 400 if no active session or no PIN generated; 401 on mismatch
     */
    @PostMapping("/check-pin")
    public ResponseEntity<?> checkPin(@RequestBody Map<String, String> payload) {
        String inputPin = payload.get("pin");

        Meeting activeMeeting = meetingRepository.findByIsCompletedFalseOrderByDateAsc()
                .stream().findFirst().orElse(null);
        if (activeMeeting == null) {
            return ResponseEntity.badRequest().body("No active meeting exists.");
        }

        String storedPin = activeMeeting.getMeetingPin();
        if (storedPin == null) {
            return ResponseEntity.badRequest().body("PIN not yet generated. Assign a secretariat leader first.");
        }

        if (storedPin.equals(inputPin)) {
            return ResponseEntity.ok("Correct");
        } else {
            return ResponseEntity.status(401).body("Incorrect PIN");
        }
    }

    /**
     * Closes the specified meeting and runs all end-of-evening cleanup.
     * Uses the meeting's own date (not the current calendar day) when checking attendance,
     * so the director can safely close a meeting the morning after it occurred.
     *
     * @param id the meeting's primary key
     * @return 200 with a summary of how many absent children had their streak reset; 400 if not found
     */
    @PostMapping("/close/{id}")
    @Transactional
    public ResponseEntity<?> closeMeeting(@PathVariable Integer id) {
        Meeting meeting = meetingRepository.findById(id).orElse(null);
        if (meeting == null) return ResponseEntity.badRequest().body("Meeting not found.");

        meeting.setIsCompleted(true);
        meetingRepository.save(meeting);

        // Decrement suspension counters only for children who were physically present
        List<Score> scoresToday = scoreRepository.findByMeetingId(id);
        List<Integer> presentChildIds = scoresToday.stream()
                .map(s -> s.getChild().getId())
                .distinct()
                .toList();
        // Suspensions of a closed season ended with it
        List<Warning> activeWarnings = warningRepository
                .findBySeasonIdAndSuspensionTrueAndRemainingMeetingsGreaterThan(seasonService.currentId(), 0);

        for (Warning w : activeWarnings) {
            if (presentChildIds.contains(w.getChild().getId())) {
                w.setRemainingMeetings(w.getRemainingMeetings() - 1);
                if (w.getRemainingMeetings() == 0) {
                    w.setSuspension(false);
                    Child c = w.getChild();
                    c.setIsSuspended(false);
                    childRepository.save(c);
                }
                warningRepository.save(w);
            }
        }

        // Reset daily points, teams, and streaks for absent children
        LocalDate meetingDate = meeting.getDate();
        List<Child> allChildren = childRepository.findAll();
        int absentsReset = 0;

        for (Child c : allChildren) {
            c.setDailyPoints(0);
            c.setCurrentTeam(null);

            boolean wasPresentToday = (c.getLastAttendanceDate() != null &&
                    c.getLastAttendanceDate().equals(meetingDate));
            if (!wasPresentToday) {
                if (c.getAttendanceStreak() != null && c.getAttendanceStreak() > 0) {
                    c.setAttendanceStreak(0);
                    absentsReset++;
                }
            }
        }

        childRepository.saveAll(allChildren);

        return ResponseEntity.ok("Meeting closed. " + absentsReset + " absent children had their streak reset.");
    }
}
