package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired private MeetingRepository meetingRepository;
    @Autowired private LeaderEvaluationRepository evaluationRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private NotificationRepository notificationRepository;

    /**
     * Citeste notele si parerile pentru o anumita seara.
     * Aduce DOAR evaluarile care sunt vizibile (active).
     */
    @GetMapping("/{meetingId}")
    public ResponseEntity<?> getFeedback(@PathVariable Integer meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null) return ResponseEntity.badRequest().build();

        // MODIFICARE: Folosim metoda noua care filtreaza stergerile
        List<LeaderEvaluation> evals = evaluationRepository.findByDateAndIsVisibleTrue(meeting.getDate());

        return ResponseEntity.ok(Map.of(
                "generalRating", meeting.getGeneralRating() != null ? meeting.getGeneralRating() : 0,
                "generalFeedback", meeting.getGeneralFeedback() != null ? meeting.getGeneralFeedback() : "",
                "evaluations", evals
        ));
    }

    /**
     * Arata istoricul notelor unui lider (doar cele vizibile).
     */
    @GetMapping("/leader/{leaderId}")
    public List<LeaderEvaluation> getLeaderHistory(@PathVariable Integer leaderId) {
        // MODIFICARE: Folosim metoda care filtreaza stergerile
        return evaluationRepository.findByLeaderIdAndIsVisibleTrueOrderByDateDesc(leaderId);
    }

    /**
     * STERGE EVALUAREA (LOGIC).
     * Nu o stergem din baza de date, doar o ascundem (is_visible = false).
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteEvaluation(@PathVariable Integer id) {
        LeaderEvaluation eval = evaluationRepository.findById(id).orElse(null);
        if(eval != null) {
            eval.setIsVisible(false); // O marcam ca "invizibila"
            evaluationRepository.save(eval); // Salvam modificarea

            // Optional: Putem recalcula media liderului aici, daca vrem sa excludem nota stearsa din medie
            recalculateLeaderRating(eval.getLeader().getId());
        }
        return ResponseEntity.ok("Evaluare ștearsă (ascunsă)!");
    }

    /**
     * Salveaza tot raportul serii.
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveFeedback(@RequestBody Map<String, Object> payload) {
        Integer meetingId = (Integer) payload.get("meetingId");
        Integer directorId = (Integer) payload.get("directorId");

        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null) return ResponseEntity.badRequest().body("Meeting not found");

        // 1. Salvam general
        meeting.setGeneralRating((Integer) payload.get("generalRating"));
        meeting.setGeneralFeedback((String) payload.get("generalFeedback"));
        meetingRepository.save(meeting);

        // 2. Salvam individual
        List<Map<String, Object>> evals = (List<Map<String, Object>>) payload.get("evaluations");

        for (Map<String, Object> evalData : evals) {
            Integer leaderId = (Integer) evalData.get("leaderId");
            Integer rating = (Integer) evalData.get("rating");
            String comment = (String) evalData.get("comment");

            LeaderEvaluation le = new LeaderEvaluation();
            le.setDate(meeting.getDate());
            le.setEvaluatedBy(directorId);
            le.setRating(rating);
            le.setComment(comment);
            le.setIsVisible(true); // Implicit vizibil

            Leader l = leaderRepository.findById(leaderId).orElse(null);
            le.setLeader(l);

            if (l != null) {
                evaluationRepository.save(le);
                recalculateLeaderRating(leaderId);

                // Notificare
                String notifMessage = String.format("📅 %s\nNota: ★%d\nFeedback: %s", meeting.getDate(), rating, comment);
                Notification n = new Notification(notifMessage, "FEEDBACK", String.valueOf(leaderId), LocalDate.now());
                notificationRepository.save(n);
            }
        }
        return ResponseEntity.ok("Feedback salvat!");
    }

    private void recalculateLeaderRating(Integer leaderId) {
        Leader leader = leaderRepository.findById(leaderId).orElse(null);
        if(leader != null) {
            // Calculam media DOAR din notele vizibile (cele sterse nu mai conteaza la medie)
            List<LeaderEvaluation> allEvals = evaluationRepository.findByLeaderIdAndIsVisibleTrueOrderByDateDesc(leaderId);

            if (!allEvals.isEmpty()) {
                double average = allEvals.stream().mapToInt(LeaderEvaluation::getRating).average().orElse(0.0);
                float roundedAvg = (float) (Math.round(average * 10.0) / 10.0);
                leader.setRating(roundedAvg);
            } else {
                leader.setRating(0.0f);
            }
            leaderRepository.save(leader);
        }
    }
}