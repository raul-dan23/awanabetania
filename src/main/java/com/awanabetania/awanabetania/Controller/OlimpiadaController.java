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

/**
 * Manages the annual Awana Olympiad competition scoring system.
 * The competition involves exactly four teams (ROSU, GALBEN, ALBASTRU, VERDE) scored
 * by two independent arbiters. Each arbiter submits placements per round; the system
 * stores them separately so the director can compare the two leaderboards and spot discrepancies.
 *
 * <p>Point values per placement: 1st=1500, 2nd=1000, 3rd=500, 4th=300.
 * Rounds can optionally be marked as "double points".</p>
 *
 * <p>Session management (create/list/close/delete) requires the admin PIN.
 * Scoring and comparison endpoints are public (access is gated by knowing the session code).</p>
 */
@RestController
@RequestMapping("/api/olimpiada")
public class OlimpiadaController {

    private static final List<String> TEAMS = List.of("ROSU", "GALBEN", "ALBASTRU", "VERDE");

    /** Points awarded per placement: place → points. */
    private static final Map<Integer, Integer> BASE_POINTS = Map.of(1, 1500, 2, 1000, 3, 500, 4, 300);

    @Autowired private OlimpiadaSessionRepository sessionRepo;
    @Autowired private OlimpiadaScoreRepository scoreRepo;

    @Value("${admin.pin}")
    private String adminPin;

    /** Returns {@code true} if the supplied PIN matches the configured admin PIN. */
    private boolean isPinValid(String pin) {
        return adminPin != null && adminPin.equals(pin);
    }

    // -------------------------------------------------------------------------
    // Session management (admin-protected)
    // -------------------------------------------------------------------------

    /**
     * Creates a new Olympiad session with a unique access code.
     *
     * @param pin  admin PIN from the {@code X-Admin-Pin} header
     * @param body JSON with "name" and "code" (max 10 characters, stored upper-case)
     * @return 200 with the saved session; 401 on invalid PIN; 400 on missing fields or duplicate code
     */
    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @RequestBody Map<String, String> body) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Incorrect PIN");

        String name = body.get("name");
        String code = body.get("code");
        if (name == null || name.isBlank() || code == null || code.isBlank())
            return ResponseEntity.badRequest().body("Name and code are required.");
        if (sessionRepo.findByCode(code.toUpperCase()).isPresent())
            return ResponseEntity.badRequest().body("This code already exists.");

        OlimpiadaSession session = new OlimpiadaSession();
        session.setName(name.trim());
        session.setCode(code.toUpperCase().trim());
        return ResponseEntity.ok(sessionRepo.save(session));
    }

    /**
     * Returns all sessions ordered by creation time descending.
     *
     * @param pin admin PIN from the {@code X-Admin-Pin} header
     * @return 200 with session list; 401 on invalid PIN
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> listSessions(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Incorrect PIN");
        return ResponseEntity.ok(sessionRepo.findAllByOrderByCreatedAtDesc());
    }

    /**
     * Closes a session so no further scores can be submitted.
     *
     * @param id  the session's primary key
     * @param pin admin PIN from the {@code X-Admin-Pin} header
     * @return 200 with the updated session; 401 on invalid PIN; 404 if not found
     */
    @PostMapping("/sessions/{id}/close")
    public ResponseEntity<?> closeSession(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Incorrect PIN");
        OlimpiadaSession session = sessionRepo.findById(id).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        session.setStatus("CLOSED");
        return ResponseEntity.ok(sessionRepo.save(session));
    }

    /**
     * Permanently deletes a session and all its associated scores.
     *
     * @param id  the session's primary key
     * @param pin admin PIN from the {@code X-Admin-Pin} header
     * @return 200 on success; 401 on invalid PIN; 404 if not found
     */
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Incorrect PIN");
        OlimpiadaSession session = sessionRepo.findById(id).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        scoreRepo.findBySessionId(id).forEach(scoreRepo::delete);
        sessionRepo.delete(session);
        return ResponseEntity.ok("Session deleted.");
    }

    // -------------------------------------------------------------------------
    // Public endpoints (access gated by session code)
    // -------------------------------------------------------------------------

    /**
     * Returns basic info for a session by code. Used by arbiters to validate the code they typed.
     *
     * @param code the session code (case-insensitive)
     * @return 200 with session info; 404 if no session has that code
     */
    @GetMapping("/session/{code}")
    public ResponseEntity<?> getSession(@PathVariable String code) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Session not found.");
        return ResponseEntity.ok(session);
    }

    /**
     * Checks whether an arbiter has already submitted scores for a specific round.
     *
     * @param code        the session code (case-insensitive)
     * @param arbiterName the arbiter's name
     * @param round       the round number to check
     * @return 200 with JSON {@code {"submitted": true/false}}; 404 if session not found
     */
    @GetMapping("/session/{code}/round-status")
    public ResponseEntity<?> roundStatus(
            @PathVariable String code,
            @RequestParam String arbiterName,
            @RequestParam Integer round) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Session not found.");
        boolean exists = scoreRepo.existsBySessionIdAndRoundNumberAndArbiterName(session.getId(), round, arbiterName);
        return ResponseEntity.ok(Map.of("submitted", exists));
    }

    /**
     * Submits an arbiter's placements for a round. Exactly four team-placement pairs are required,
     * each team and each place appearing exactly once. Re-submitting a round replaces the previous entry.
     *
     * @param code the session code (case-insensitive)
     * @param body JSON with "arbiterName", "roundNumber", "isDouble", and "scores"
     *             (list of {team, place} objects)
     * @return 200 on success; 400 on validation failure; 404 if session not found
     */
    @PostMapping("/session/{code}/score")
    public ResponseEntity<?> submitScore(
            @PathVariable String code,
            @RequestBody Map<String, Object> body) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Session not found.");
        if ("CLOSED".equals(session.getStatus()))
            return ResponseEntity.badRequest().body("Session is closed.");

        String arbiterName = (String) body.get("arbiterName");
        Integer roundNumber = (Integer) body.get("roundNumber");
        boolean isDouble = Boolean.TRUE.equals(body.get("isDouble"));

        if (arbiterName == null || arbiterName.isBlank() || roundNumber == null)
            return ResponseEntity.badRequest().body("Arbiter name and round number are required.");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) body.get("scores");
        if (scores == null || scores.size() != 4)
            return ResponseEntity.badRequest().body("Exactly 4 scores must be submitted.");

        // Validate unique places and valid team names
        Set<Integer> places = new HashSet<>();
        Set<String> teams = new HashSet<>();
        for (Map<String, Object> s : scores) {
            String team = (String) s.get("team");
            Integer place = (Integer) s.get("place");
            if (!TEAMS.contains(team)) return ResponseEntity.badRequest().body("Invalid team: " + team);
            if (place < 1 || place > 4) return ResponseEntity.badRequest().body("Place must be 1–4.");
            if (!places.add(place)) return ResponseEntity.badRequest().body("Places must be unique.");
            if (!teams.add(team)) return ResponseEntity.badRequest().body("Teams must be unique.");
        }

        // Replace any existing submission for this arbiter/round
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

    /**
     * Submits extra (bonus) points for a specific team, not tied to a placement round.
     * These are stored with {@code roundNumber=0} and {@code place=0} to distinguish them.
     *
     * @param code the session code (case-insensitive)
     * @param body JSON with "arbiterName", "team", "points", and optional "note"
     * @return 200 on success; 400 on validation failure; 404 if session not found
     */
    @PostMapping("/session/{code}/extra")
    public ResponseEntity<?> submitExtra(
            @PathVariable String code,
            @RequestBody Map<String, Object> body) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Session not found.");
        if ("CLOSED".equals(session.getStatus()))
            return ResponseEntity.badRequest().body("Session is closed.");

        String arbiterName = (String) body.get("arbiterName");
        String team = (String) body.get("team");
        Integer points = (Integer) body.get("points");
        String note = (String) body.getOrDefault("note", "");

        if (arbiterName == null || arbiterName.isBlank())
            return ResponseEntity.badRequest().body("Arbiter name is required.");
        if (!TEAMS.contains(team))
            return ResponseEntity.badRequest().body("Invalid team.");
        if (points == null || points <= 0)
            return ResponseEntity.badRequest().body("Points must be positive.");

        OlimpiadaScore score = new OlimpiadaScore();
        score.setSessionId(session.getId());
        score.setRoundNumber(0);
        score.setTeam(team);
        score.setArbiterName(arbiterName);
        score.setPlace(0);
        score.setPoints(points);
        score.setIsDouble(false);
        score.setNote(note.isBlank() ? null : note.trim());
        scoreRepo.save(score);

        return ResponseEntity.ok(Map.of("saved", true));
    }

    /**
     * Deletes all scores submitted by a specific arbiter for a specific round.
     * Used by the director to allow an arbiter to re-submit a corrected round.
     *
     * @param code        the session code (case-insensitive)
     * @param round       the round number to delete
     * @param arbiterName the arbiter whose submission should be removed
     * @param pin         admin PIN from the {@code X-Admin-Pin} header
     * @return 200 on success; 401 on invalid PIN; 404 if session not found
     */
    @DeleteMapping("/session/{code}/round/{round}/arbiter/{arbiterName}")
    public ResponseEntity<?> deleteRound(
            @PathVariable String code,
            @PathVariable Integer round,
            @PathVariable String arbiterName,
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Incorrect PIN");
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Session not found.");
        scoreRepo.deleteBySessionIdAndRoundNumberAndArbiterName(session.getId(), round, arbiterName);
        return ResponseEntity.ok("Round deleted.");
    }

    /**
     * Returns a full comparison report for a session: per-arbiter leaderboards, round breakdowns,
     * and extra-points entries. The frontend uses this to highlight discrepancies between arbiters.
     *
     * @param code the session code (case-insensitive)
     * @return 200 with a structured comparison map; 404 if session not found
     */
    @GetMapping("/session/{code}/compare")
    public ResponseEntity<?> compare(@PathVariable String code) {
        OlimpiadaSession session = sessionRepo.findByCode(code.toUpperCase()).orElse(null);
        if (session == null) return ResponseEntity.status(404).body("Session not found.");

        List<OlimpiadaScore> allScores = scoreRepo.findBySessionId(session.getId());

        List<String> arbiters = allScores.stream()
                .map(OlimpiadaScore::getArbiterName)
                .distinct().sorted().collect(Collectors.toList());

        Map<String, Object> arbiterData = new LinkedHashMap<>();
        for (String arbiter : arbiters) {
            List<OlimpiadaScore> arbScores = allScores.stream()
                    .filter(s -> s.getArbiterName().equals(arbiter))
                    .collect(Collectors.toList());

            // Leaderboard: sum all points per team (includes both placement and extra)
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

            // Rounds: placement entries (place > 0), grouped by round number
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
                        Map<String, Object> scoresMap = new LinkedHashMap<>();
                        for (OlimpiadaScore s : e.getValue()) {
                            Map<String, Object> sc = new LinkedHashMap<>();
                            sc.put("place", s.getPlace());
                            sc.put("points", s.getPoints());
                            scoresMap.put(s.getTeam(), sc);
                        }
                        r.put("scores", scoresMap);
                        return r;
                    }).collect(Collectors.toList());

            // Extras: entries with place == 0
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
