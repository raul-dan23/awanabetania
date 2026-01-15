package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aceasta clasa se ocupa de Echipe si Jocuri.
 * Varianta SIMPLIFICATA:
 * 1. Echipele sunt stocate direct pe copil (Child.currentTeam).
 * 2. Punctele individuale sunt luate din Child.dailyPoints.
 * 3. Punctele de joc sunt luate din TeamGamePoint.
 */
@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
public class TeamController {

    @Autowired private ChildRepository childRepository;
    @Autowired private TeamGamePointRepository teamGamePointRepository; // Punctele de la jocuri
    @Autowired private MeetingRepository meetingRepository;

    /**
     * Lista cu copiii care stau pe banca si asteapta sa fie alesi.
     * Sunt afisati doar cei care NU au campul 'currentTeam' setat si nu sunt suspendati.
     */
    @GetMapping("/available")
    public List<Child> getAvailableChildren() {
        return childRepository.findAll().stream()
                // Conditia 1: Sa nu aiba echipa setata pe ziua de azi
                .filter(c -> c.getCurrentTeam() == null || c.getCurrentTeam().isEmpty())
                // Conditia 2: Sa nu fie suspendati
                .filter(c -> !Boolean.TRUE.equals(c.getIsSuspended()))
                // Ordonare alfabetica
                .sorted(Comparator.comparing(Child::getName))
                .collect(Collectors.toList());
    }

    /**
     * Calculeaza scorul total al unei echipe in timp real.
     * Formula: Suma(dailyPoints ale copiilor din echipa) + Suma(TeamGamePoints ale echipei).
     */
    @GetMapping("/status/{color}")
    public Map<String, Object> getTeamStatus(@PathVariable String color) {
        Map<String, Object> response = new HashMap<>();
        Meeting activeMeeting = getActiveMeeting();

        // Pasul A: Gasim membrii echipei (cautam in tabelul de copii cine are echipa asta)
        List<Child> members = childRepository.findAll().stream()
                .filter(c -> c.getCurrentTeam() != null && c.getCurrentTeam().equalsIgnoreCase(color))
                .toList();

        // Pasul B: Calculam suma punctelor lor ZILNICE (individuale)
        // Nu mai cautam in ScoreRepository, ci direct in dailyPoints care se reseteaza seara
        int individualSum = members.stream()
                .mapToInt(c -> c.getDailyPoints() != null ? c.getDailyPoints() : 0)
                .sum();

        // Pasul C: Calculam punctele de la JOCURI (doar daca e o sesiune activa)
        int gameSum = 0;
        if (activeMeeting != null) {
            List<TeamGamePoint> gamePoints = teamGamePointRepository.findByMeetingIdAndTeamColor(activeMeeting.getId(), color);
            gameSum = gamePoints.stream().mapToInt(TeamGamePoint::getPoints).sum();
        }

        // Impachetam rezultatele
        response.put("members", members);
        response.put("individualScore", individualSum);
        response.put("gameScore", gameSum);
        response.put("totalScore", individualSum + gameSum);

        return response;
    }

    /**
     * Alege un copil si il baga intr-o echipa.
     * Actualizeaza direct campul 'currentTeam' al copilului.
     */
    @PostMapping("/pick")
    public ResponseEntity<?> pickChild(@RequestBody Map<String, String> payload) {
        try {
            Integer childId = Integer.parseInt(payload.get("childId"));
            String teamColor = payload.get("teamColor"); // ex: "red"

            Child child = childRepository.findById(childId).orElse(null);
            if (child == null) return ResponseEntity.badRequest().body("Copil invalid");

            // Il asignam in echipa
            child.setCurrentTeam(teamColor);
            childRepository.save(child);

            return ResponseEntity.ok("Adaugat in " + teamColor);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Eroare: " + e.getMessage());
        }
    }

    /**
     * Salveaza rezultatul unei runde de joc (ex: Trasul Franghiei).
     * Creeaza un TeamGamePoint legat de intalnirea de azi.
     */
    @PostMapping("/game-round")
    public ResponseEntity<?> saveGameRound(@RequestBody Map<String, Object> payload) {
        Meeting activeMeeting = getActiveMeeting();
        if (activeMeeting == null) return ResponseEntity.badRequest().body("Nu exista o sesiune activa!");

        List<String> ranking = (List<String>) payload.get("ranking"); // ex: ["red", "blue", "green", "yellow"]
        Boolean isDouble = (Boolean) payload.get("isDouble");

        int[] standardPoints = {1000, 500, 300, 100}; // Locul 1, 2, 3, 4

        for (int i = 0; i < ranking.size(); i++) {
            String color = ranking.get(i);
            int pts = (i < standardPoints.length) ? standardPoints[i] : 0;

            // Daca e runda dubla, punctele se inmultesc cu 2
            if (Boolean.TRUE.equals(isDouble)) pts *= 2;

            TeamGamePoint tp = new TeamGamePoint();
            tp.setMeeting(activeMeeting);
            tp.setTeamColor(color);
            tp.setPoints(pts);
            teamGamePointRepository.save(tp);
        }

        return ResponseEntity.ok("Joc salvat!");
    }

    /**
     * Metoda ajutatoare.
     * Gaseste prima intalnire din calendar care este pornita si nefinalizata.
     */
    private Meeting getActiveMeeting() {
        return meetingRepository.findAll().stream()
                .filter(m -> m.getIsCompleted() == null || !m.getIsCompleted())
                .findFirst()
                .orElse(null);
    }
}