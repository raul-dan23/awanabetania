package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Model.Score;
import com.awanabetania.awanabetania.Model.ScoreRequest;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.MeetingRepository;
import com.awanabetania.awanabetania.Repository.ScoreRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Aceasta clasa este casieria aplicatiei.
 * Aici liderii dau puncte copiilor pentru prezenta, uniforma si versete.
 * Se ocupa de calculul matematic si salvarea in "pusculita" copilului si a echipei.
 */
@RestController
@RequestMapping("/api/scores")
@CrossOrigin(origins = "*")
public class ScoreController {

    @Autowired private ScoreRepository scoreRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private MeetingRepository meetingRepository;

    /**
     * Metoda principala de adaugare puncte.
     * Se apeleaza cand liderul apasa butonul Salveaza in aplicatie.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addScore(@RequestBody ScoreRequest request) {

        // 1. Cautam o intalnire activa (neincheiata)
        // Avem nevoie de ea ca sa stim pe ce data punem punctele
        Meeting meeting = meetingRepository.findAll().stream()
                .filter(m -> m.getIsCompleted() == null || !m.getIsCompleted())
                .findFirst()
                .orElse(null);

        if (meeting == null) {
            return ResponseEntity.badRequest().body("Nu există nicio sesiune activă în Calendar!");
        }

        // 2. Cautam copilul caruia vrem sa ii dam puncte
        Child child = childRepository.findById(request.getChildId()).orElse(null);
        if (child == null) return ResponseEntity.badRequest().body("Copil invalid");

        // 3. Verificam daca a fost deja punctat la aceasta intalnire
        // Nu vrem sa ii dam puncte de doua ori din greseala
        boolean alreadyScored = scoreRepository.findAll().stream()
                .anyMatch(s -> s.getChild().getId().equals(child.getId()) && s.getMeeting().getId().equals(meeting.getId()));

        if (alreadyScored) {
            return ResponseEntity.badRequest().body("Acest copil a primit deja punctele pentru sesiunea curentă!");
        }

        // 4. Pregatim obiectul SCORE (Istoricul)
        Score score = new Score();
        score.setChild(child);
        score.setMeeting(meeting);
        score.setDate(LocalDate.now());

        // Setam bifele primite din aplicatie (ce a facut copilul)
        score.setAttended(Boolean.TRUE.equals(request.getAttended()));
        score.setHasBible(Boolean.TRUE.equals(request.getHasBible()));
        score.setHasHandbook(Boolean.TRUE.equals(request.getHasHandbook()));
        score.setLesson(Boolean.TRUE.equals(request.getLesson()));
        score.setFriend(Boolean.TRUE.equals(request.getFriend()));
        score.setHasUniform(Boolean.TRUE.equals(request.getHasUniform()));
        score.setExtraPoints(request.getExtraPoints() != null ? request.getExtraPoints() : 0);

        // 5. Calculam punctele matematic (ex: Uniforma = 10.000)
        int points = calculatePoints(score);
        String details = generateDetailsString(score);

        // =================================================================================
        // LOGICA DE SALVARE A PUNCTELOR (SIMPLIFICATA)
        // =================================================================================

        // A. Salvam in ISTORIC (ca sa stim pe ce s-au dat punctele in viitor)
        score.setIndividualPoints(points);
        score.setTotal(points);
        score.setDetails(details);

        // B. Salvam la SEZON (Banii copilului - raman permanent)
        int oldSeasonPoints = child.getSeasonPoints() == null ? 0 : child.getSeasonPoints();
        child.setSeasonPoints(oldSeasonPoints + points);

        // C. Salvam la ZI (Pentru ECHIPA - se sterg la "Incheie Seara")
        int oldDailyPoints = child.getDailyPoints() == null ? 0 : child.getDailyPoints();
        child.setDailyPoints(oldDailyPoints + points);

        // =================================================================================

        // 6. Actualizam statistica de prezenta si inventarul
        if (Boolean.TRUE.equals(request.getAttended())) {
            child.setTotalAttendance((child.getTotalAttendance() == null ? 0 : child.getTotalAttendance()) + 1);
            child.setAttendanceStreak((child.getAttendanceStreak() == null ? 0 : child.getAttendanceStreak()) + 1);

            if(Boolean.TRUE.equals(request.getLesson())) {
                child.setLessonsCompleted((child.getLessonsCompleted() == null ? 0 : child.getLessonsCompleted()) + 1);
            }
            // Daca a venit cu uniforma, inseamna ca are uniforma acasa
            if(Boolean.TRUE.equals(request.getHasUniform())) child.setHasShirt(true);
            if(Boolean.TRUE.equals(request.getHasBible())) child.setHasManual(true);
        }

        // 7. Salvam ambele entitati in baza de date
        scoreRepository.save(score);
        childRepository.save(child);

        return ResponseEntity.ok("Puncte salvate! Total: " + points);
    }

    /**
     * Arata istoricul punctelor pentru un singur copil.
     * Folosit in pagina de Profil.
     */
    @GetMapping("/child/{childId}")
    public List<Score> getScoresByChild(@PathVariable Integer childId) {
        return scoreRepository.findAll().stream()
                .filter(s -> s.getChild().getId().equals(childId))
                .sorted((a, b) -> b.getMeeting().getDate().compareTo(a.getMeeting().getDate()))
                .toList();
    }

    /**
     * Matematica punctelor. Aici definim cat valoreaza fiecare lucru.
     */
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

    /**
     * Genereaza un text explicativ (ex: "Prezenta, Uniforma")
     */
    private String generateDetailsString(Score s) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(s.getAttended())) sb.append("Prezenta, ");
        if (Boolean.TRUE.equals(s.getHasBible())) sb.append("Biblie, ");
        if (Boolean.TRUE.equals(s.getHasHandbook())) sb.append("Manual, ");
        if (Boolean.TRUE.equals(s.getLesson())) sb.append("Lectie, ");
        if (Boolean.TRUE.equals(s.getFriend())) sb.append("Prieten, ");
        if (Boolean.TRUE.equals(s.getHasUniform())) sb.append("Uniforma, ");
        if (s.getExtraPoints() != null && s.getExtraPoints() > 0) sb.append("Extra (+").append(s.getExtraPoints()).append("), ");

        if (sb.length() > 2) {
            return sb.substring(0, sb.length() - 2);
        }
        return "Puncte acordate";
    }
}