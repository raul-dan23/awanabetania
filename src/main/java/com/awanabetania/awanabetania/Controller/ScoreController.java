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

@RestController
@RequestMapping("/api/scores")
@CrossOrigin(origins = "*")
public class ScoreController {

    @Autowired private ScoreRepository scoreRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private NotificationRepository notificationRepository;

    @PostMapping("/add")
    public ResponseEntity<?> addScore(@RequestBody ScoreRequest request) {

        // 1. Validări standard (Sesiune activă, Copil valid, Dubluri)
        // Luam sedinta nesfinalizata cu cea mai APROPIATA data (nu prima din findAll care e nesortata)
        Meeting meeting = meetingRepository.findByIsCompletedFalseOrderByDateAsc()
                .stream().findFirst().orElse(null);

        if (meeting == null) return ResponseEntity.badRequest().body("Nu există sesiune activă!");

        Child child = childRepository.findById(request.getChildId()).orElse(null);
        if (child == null) return ResponseEntity.badRequest().body("Copil invalid");

        boolean alreadyScored = scoreRepository.findAll().stream()
                .anyMatch(s -> s.getChild().getId().equals(child.getId()) && s.getMeeting().getId().equals(meeting.getId()));

        if (alreadyScored) return ResponseEntity.badRequest().body("Copilul a fost deja punctat azi!");

        // 2. Creare Score
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

        // Update puncte
        child.setSeasonPoints((child.getSeasonPoints() == null ? 0 : child.getSeasonPoints()) + points);
        child.setDailyPoints((child.getDailyPoints() == null ? 0 : child.getDailyPoints()) + points);

        // =================================================================================
        // 3. LOGICA DE PREZENȚĂ ȘI PREMII
        // =================================================================================
        if (Boolean.TRUE.equals(request.getAttended())) {

            // A. Incrementăm Streak-ul
            int currentStreak = (child.getAttendanceStreak() == null) ? 0 : child.getAttendanceStreak();
            int newStreak = currentStreak + 1;
            child.setAttendanceStreak(newStreak);

            // B. Setam data SEDINTEI (nu LocalDate.now()) ca sa fie consistent cu logica de streak la inchidere
            child.setLastAttendanceDate(meeting.getDate());

            // C. Totaluri și Lecții
            child.setTotalAttendance((child.getTotalAttendance() == null ? 0 : child.getTotalAttendance()) + 1);
            if(Boolean.TRUE.equals(request.getLesson())) {
                child.setLessonsCompleted((child.getLessonsCompleted() == null ? 0 : child.getLessonsCompleted()) + 1);
            }

            // Liniile care dădeau automat tricoul și manualul au fost șterse de aici.

            // D. Verificare Premii (Notificări)
            checkRewards(child, newStreak);
        }

        scoreRepository.save(score);
        childRepository.save(child);

        return ResponseEntity.ok("Puncte salvate! Total: " + points);
    }

    // Helper verificare premii
    private void checkRewards(Child child, int streak) {
        if (streak == 5 && (child.getHasShirt() == null || !child.getHasShirt())) {
            createNotification(child, "SHIRT_ELIGIBLE", "🎁 DIRECTOR! " + child.getName() + " așteaptă TRICOUL (5 prezențe)!");
        }
        if (streak == 10 && (child.getHasHat() == null || !child.getHasHat())) {
            createNotification(child, "HAT_ELIGIBLE", "🎁 DIRECTOR! " + child.getName() + " așteaptă CĂCIULA (10 prezențe)!");
        }
    }

    private void createNotification(Child child, String type, String msg) {
        // Evităm duplicatele active
        List<Notification> existing = notificationRepository.findActiveByChildAndType(child.getId(), type);
        if (existing.isEmpty()) {
            Notification n = new Notification();
            n.setMessage(msg); n.setType(type); n.setVisibleTo("DIRECTOR"); n.setDate(LocalDate.now()); n.setIsVisible(true); n.setChildId(child.getId());
            notificationRepository.save(n);
        }
    }

    @GetMapping("/child/{childId}")
    public List<Score> getScoresByChild(@PathVariable Integer childId) {
        return scoreRepository.findAll().stream()
                .filter(s -> s.getChild().getId().equals(childId))
                .sorted((a, b) -> b.getMeeting().getDate().compareTo(a.getMeeting().getDate()))
                .toList();
    }

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

    private String generateDetailsString(Score s) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(s.getAttended())) sb.append("Prezenta, ");
        if (Boolean.TRUE.equals(s.getHasBible())) sb.append("Biblie, ");
        if (Boolean.TRUE.equals(s.getHasHandbook())) sb.append("Manual, ");
        if (Boolean.TRUE.equals(s.getLesson())) sb.append("Lectie, ");
        if (Boolean.TRUE.equals(s.getFriend())) sb.append("Prieten, ");
        if (Boolean.TRUE.equals(s.getHasUniform())) sb.append("Uniforma, ");
        if (s.getExtraPoints() != null && s.getExtraPoints() > 0) sb.append("Extra (+").append(s.getExtraPoints()).append("), ");
        return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "Puncte acordate";
    }
}