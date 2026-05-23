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
    private static final Map<Integer, Integer> BASE_POINTS = Map.of(1, 1500, 2, 1000, 3, 500, 4, 300);

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

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Integer id,
                                           @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("PIN incorect");
        OlimpiadaSession session = sessionRepo.findById(id).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        scoreRepo.findBySessionId(id).forEach(scoreRepo::delete);
        sessionRepo.delete(session);
        return ResponseEntity.ok("Stearsa");
    }

    // --- Acces public prin cod ---

    @GetMapping("/session/{code}")
    public ResponseEntity<?> getSession(@PathVariable String code) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");
        return ResponseEntity.ok(session);
    }

    @GetMapping("/session/{code}/round-status")
    public ResponseEntity<?> roundStatus(@PathVariable String code,
                                         @RequestParam String arbiterName,
                                         @RequestParam Integer round) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");
        boolean exists = scoreRepo.existsBySessionIdAndRoundNumberAndArbiterName(session.getId(), round, arbiterName);
        return ResponseEntity.ok(Map.of("submitted", exists));
    }

    // Trimite scorurile unui tur (4 echipe + optional double points)
    @PostMapping("/session/{code}/score")
    public ResponseEntity<?> submitScore(@PathVariable String code,
                                         @RequestBody Map<String, Object> body) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");
        if ("CLOSED".equals(session.getStatus()))
            return ResponseEntity.badRequest().body("Sesiunea este inchisa");

        String arbiterName = (String) body.get("arbiterName");
        Integer roundNumber = (Integer) body.get("roundNumber");
        boolean isDouble = Boolean.TRUE.equals(body.get("isDouble"));

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

        scoreRepo.deleteBySessionIdAndRoundNumberAndArbiterName(session.getId(), roundNumber, arbiterName);

        for (Map<String, Object> s : scores) {
            OlimpiadaScore score = new OlimpiadaScore();
            score.setSessionId(session.getId());
            score.setRoundNumber(roundNumber);
            score.setTeam((String) s.get("team"));
            score.setArbiterName(arbiterName);
            Integer place = (Integer) s.get("place");
            score.setPlace(place);
            int pts = BASE_POINTS.get(place);
            score.setPoints(isDouble ? pts * 2 : pts);
            score.setIsDouble(isDouble);
            scoreRepo.save(score);
        }

        return ResponseEntity.ok(Map.of("saved", 4, "round", roundNumber, "isDouble", isDouble));
    }

    // Puncte extra (fara plasament, pentru echipa specifica)
    @PostMapping("/session/{code}/extra")
    public ResponseEntity<?> submitExtra(@PathVariable String code,
                                         @RequestBody Map<String, Object> body) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");
        if ("CLOSED".equals(session.getStatus()))
            return ResponseEntity.badRequest().body("Sesiunea este inchisa");

        String arbiterName = (String) body.get("arbiterName");
        String team = (String) body.get("team");
        Integer points = (Integer) body.get("points");
        String note = (String) body.getOrDefault("note", "");

        if (arbiterName == null || arbiterName.isBlank())
            return ResponseEntity.badRequest().body("Arbitrul este obligatoriu");
        if (!TEAMS.contains(team))
            return ResponseEntity.badRequest().body("Echipa invalida");
        if (points == null || points <= 0)
            return ResponseEntity.badRequest().body("Punctele trebuie sa fie pozitive");

        OlimpiadaScore score = new OlimpiadaScore();
        score.setSessionId(session.getId());
        score.setRoundNumber(0); // 0 = extra points
        score.setTeam(team);
        score.setArbiterName(arbiterName);
        score.setPlace(0); // 0 = extra, not a placement
        score.setPoints(points);
        score.setIsDouble(false);
        score.setNote(note.isBlank() ? null : note.trim());
        scoreRepo.save(score);

        return ResponseEntity.ok(Map.of("saved", true));
    }

    // Sterge un tur al unui arbitru
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

    // Comparatie — N leaderboard-uri separate per arbitru + dosare cu runde
    @GetMapping("/session/{code}/compare")
    public ResponseEntity<?> compare(@PathVariable String code) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Sesiunea nu exista");

        List<OlimpiadaScore> allScores = scoreRepo.findBySessionId(session.getId());

        List<String> arbiters = allScores.stream()
                .map(OlimpiadaScore::getArbiterName)
                .distinct().sorted().collect(Collectors.toList());

        // Per arbitru: leaderboard + runde + extra
        Map<String, Object> arbiterData = new LinkedHashMap<>();
        for (String arbiter : arbiters) {
            List<OlimpiadaScore> arbScores = allScores.stream()
                    .filter(s -> s.getArbiterName().equals(arbiter))
                    .collect(Collectors.toList());

            // Leaderboard: totaluri per echipa (doar scoruri cu place > 0)
            Map<String, Integer> leaderboard = new LinkedHashMap<>();
            for (String team : TEAMS) {
                int total = arbScores.stream()
                        .filter(s -> s.getTeam().equals(team))
                        .mapToInt(OlimpiadaScore::getPoints)
                        .sum();
                leaderboard.put(team, total);
            }
            List<Map<String, Object>> leaderboardSorted = leaderboard.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("team", e.getKey());
                        m.put("total", e.getValue());
                        return m;
                    }).collect(Collectors.toList());

            // Runde (place > 0), grupate pe roundNumber
            Map<Integer, List<OlimpiadaScore>> byRound = arbScores.stream()
                    .filter(s -> s.getPlace() > 0)
                    .collect(Collectors.groupingBy(OlimpiadaScore::getRoundNumber));

            List<Map<String, Object>> rounds = byRound.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("round", e.getKey());
                        boolean dbl = e.getValue().stream().anyMatch(s -> Boolean.TRUE.equals(s.getIsDouble()));
                        r.put("isDouble", dbl);
                        Map<String, Object> scores = new LinkedHashMap<>();
                        for (OlimpiadaScore s : e.getValue()) {
                            Map<String, Object> sc = new LinkedHashMap<>();
                            sc.put("place", s.getPlace());
                            sc.put("points", s.getPoints());
                            scores.put(s.getTeam(), sc);
                        }
                        r.put("scores", scores);
                        return r;
                    }).collect(Collectors.toList());

            // Extra (place == 0)
            List<Map<String, Object>> extras = arbScores.stream()
                    .filter(s -> s.getPlace() == 0)
                    .map(s -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("team", s.getTeam());
                        m.put("points", s.getPoints());
                        m.put("note", s.getNote() != null ? s.getNote() : "");
                        m.put("createdAt", s.getCreatedAt());
                        return m;
                    }).collect(Collectors.toList());

            Map<String, Object> ad = new LinkedHashMap<>();
            ad.put("leaderboard", leaderboardSorted);
            ad.put("rounds", rounds);
            ad.put("extras", extras);
            ad.put("roundCount", byRound.size());
            arbiterData.put(arbiter, ad);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("sessionName", session.getName());
        result.put("sessionCode", session.getCode());
        result.put("status", session.getStatus());
        result.put("arbiters", arbiters);
        result.put("arbiterData", arbiterData);
        result.put("teams", TEAMS);

        return ResponseEntity.ok(result);
    }
}
