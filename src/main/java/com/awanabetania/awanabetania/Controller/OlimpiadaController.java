package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.OlimpiadaScore;
import com.awanabetania.awanabetania.Model.OlimpiadaSession;
import com.awanabetania.awanabetania.Repository.OlimpiadaScoreRepository;
import com.awanabetania.awanabetania.Repository.OlimpiadaSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/olimpiada")
@CrossOrigin(origins = "*")
public class OlimpiadaController {

    private static final List<String> TEAMS = List.of("ROSU", "GALBEN", "ALBASTRU", "VERDE");

    @Autowired private OlimpiadaSessionRepository sessionRepo;
    @Autowired private OlimpiadaScoreRepository scoreRepo;

    @Value("${admin.pin}")
    private String adminPin;

    private boolean isPinValid(String pin) {
        return adminPin != null && adminPin.equals(pin);
    }

    // --- Sesiuni ---

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestHeader(value = "X-Admin-Pin", required = false) String pin,
                                           @RequestBody Map<String, String> body) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("PIN incorect");
        String name = body.get("name");
        String code = body.get("code");
        if (name == null || name.isBlank() || code == null || code.isBlank())
            return ResponseEntity.badRequest().body("Numele si codul sunt obligatorii");
        if (sessionRepo.findByCode(code.toUpperCase()).isPresent())
            return ResponseEntity.badRequest().body("Codul exista deja");

        OlimpiadaSession session = new OlimpiadaSession();
        session.setName(name.trim());
        session.setCode(code.toUpperCase().trim());
        return ResponseEntity.ok(sessionRepo.save(session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> listSessions(@RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("PIN incorect");
        return ResponseEntity.ok(sessionRepo.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/sessions/{id}/close")
    public ResponseEntity<?> closeSession(@PathVariable Integer id,
                                          @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("PIN incorect");
        OlimpiadaSession session = sessionRepo.findById(id).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        session.setStatus("CLOSED");
        return ResponseEntity.ok(sessionRepo.save(session));
    }

    // --- Acces public prin cod ---

    @GetMapping("/session/{code}")
    public ResponseEntity<?> getSession(@PathVariable String code) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");
        return ResponseEntity.ok(session);
    }

    // Verifica daca un arbitru a introdus deja un anumit tur
    @GetMapping("/session/{code}/round-status")
    public ResponseEntity<?> roundStatus(@PathVariable String code,
                                         @RequestParam String arbiterName,
                                         @RequestParam Integer round) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");
        boolean exists = scoreRepo.existsBySessionIdAndRoundNumberAndArbiterName(session.getId(), round, arbiterName);
        return ResponseEntity.ok(Map.of("submitted", exists));
    }

    // Trimite scorurile unui tur (4 echipe, locurile 1-4)
    @PostMapping("/session/{code}/score")
    public ResponseEntity<?> submitScore(@PathVariable String code,
                                         @RequestBody Map<String, Object> body) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");
        if ("CLOSED".equals(session.getStatus()))
            return ResponseEntity.badRequest().body("Sesiunea este inchisa");

        String arbiterName = (String) body.get("arbiterName");
        Integer roundNumber = (Integer) body.get("roundNumber");

        if (arbiterName == null || arbiterName.isBlank() || roundNumber == null)
            return ResponseEntity.badRequest().body("Arbitru si runda sunt obligatorii");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) body.get("scores");
        if (scores == null || scores.size() != 4)
            return ResponseEntity.badRequest().body("Trebuie trimise exact 4 scoruri");

        Set<Integer> places = new HashSet<>();
        Set<String> teams = new HashSet<>();
        for (Map<String, Object> s : scores) {
            String team = (String) s.get("team");
            Integer place = (Integer) s.get("place");
            if (!TEAMS.contains(team)) return ResponseEntity.badRequest().body("Echipa invalida: " + team);
            if (place < 1 || place > 4) return ResponseEntity.badRequest().body("Locul trebuie sa fie 1-4");
            if (!places.add(place)) return ResponseEntity.badRequest().body("Locurile trebuie sa fie unice");
            if (!teams.add(team)) return ResponseEntity.badRequest().body("Echipele trebuie sa fie unice");
        }

        // Sterge scorurile existente pentru acest tur+arbitru (override)
        scoreRepo.deleteBySessionIdAndRoundNumberAndArbiterName(session.getId(), roundNumber, arbiterName);

        List<OlimpiadaScore> saved = new ArrayList<>();
        for (Map<String, Object> s : scores) {
            OlimpiadaScore score = new OlimpiadaScore();
            score.setSessionId(session.getId());
            score.setRoundNumber(roundNumber);
            score.setTeam((String) s.get("team"));
            score.setArbiterName(arbiterName);
            Integer place = (Integer) s.get("place");
            score.setPlace(place);
            score.setPoints(5 - place); // 1st=4, 2nd=3, 3rd=2, 4th=1
            saved.add(scoreRepo.save(score));
        }

        return ResponseEntity.ok(Map.of("saved", saved.size(), "round", roundNumber));
    }

    // Sterge un tur al unui arbitru (undo)
    @DeleteMapping("/session/{code}/round/{round}/arbiter/{arbiterName}")
    public ResponseEntity<?> deleteRound(@PathVariable String code,
                                         @PathVariable Integer round,
                                         @PathVariable String arbiterName,
                                         @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("PIN incorect");
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");
        scoreRepo.deleteBySessionIdAndRoundNumberAndArbiterName(session.getId(), round, arbiterName);
        return ResponseEntity.ok("Stears");
    }

    // Comparatie totale per echipa per arbitru
    @GetMapping("/session/{code}/compare")
    public ResponseEntity<?> compare(@PathVariable String code) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");

        List<OlimpiadaScore> allScores = scoreRepo.findBySessionId(session.getId());

        // Colecteaza arbitrii unici
        List<String> arbiters = allScores.stream()
                .map(OlimpiadaScore::getArbiterName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Totaluri per echipa per arbitru
        Map<String, Map<String, Integer>> totals = new LinkedHashMap<>();
        for (String team : TEAMS) {
            Map<String, Integer> arbiterTotals = new LinkedHashMap<>();
            for (String arbiter : arbiters) {
                int total = allScores.stream()
                        .filter(s -> s.getTeam().equals(team) && s.getArbiterName().equals(arbiter))
                        .mapToInt(OlimpiadaScore::getPoints)
                        .sum();
                arbiterTotals.put(arbiter, total);
            }
            totals.put(team, arbiterTotals);
        }

        // Numarul de tururi per arbitru
        Map<String, Long> roundCounts = new LinkedHashMap<>();
        for (String arbiter : arbiters) {
            long count = allScores.stream()
                    .filter(s -> s.getArbiterName().equals(arbiter))
                    .map(OlimpiadaScore::getRoundNumber)
                    .distinct()
                    .count();
            roundCounts.put(arbiter, count);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("sessionName", session.getName());
        result.put("sessionCode", session.getCode());
        result.put("status", session.getStatus());
        result.put("arbiters", arbiters);
        result.put("totals", totals);
        result.put("roundCounts", roundCounts);
        result.put("teams", TEAMS);

        return ResponseEntity.ok(result);
    }
}
