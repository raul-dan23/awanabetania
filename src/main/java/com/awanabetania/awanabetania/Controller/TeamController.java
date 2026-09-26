package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Manages team selection and real-time scoring during the games portion of each meeting.
 * Teams are stored directly on the {@link Child} entity via {@code currentTeam}.
 * Individual scores are read from {@code Child.dailyPoints} (reset when the meeting closes).
 * Game-round scores are stored separately as {@link TeamGamePoint} records linked to the meeting.
 *
 * <p>Team color identifiers are free-form strings (e.g. "red", "blue", "green", "yellow").</p>
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired private ChildRepository childRepository;
    @Autowired private TeamGamePointRepository teamGamePointRepository;
    @Autowired private MeetingRepository meetingRepository;

    /**
     * Returns all children who are not currently assigned to a team and are not suspended,
     * ordered alphabetically.
     *
     * @return list of available (bench) children
     */
    @GetMapping("/available")
    public List<Child> getAvailableChildren() {
        return childRepository.findAvailableChildren();
    }

    /**
     * Removes a child from their current team, placing them back on the bench.
     *
     * @param payload JSON with key "childId" (as a string)
     * @return 200 with the team name the child was removed from; 400 on invalid ID or error
     */
    @PostMapping("/remove")
    public ResponseEntity<?> removeChildFromTeam(@RequestBody Map<String, String> payload) {
        try {
            Integer childId = Integer.parseInt(payload.get("childId"));
            Child child = childRepository.findById(childId).orElse(null);
            if (child == null) return ResponseEntity.badRequest().body("Invalid child.");

            String oldTeam = child.getCurrentTeam();
            child.setCurrentTeam(null);
            childRepository.save(child);

            return ResponseEntity.ok("Removed from team " + oldTeam);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Returns the current real-time score breakdown for a team.
     * Total score = sum of members' daily points + sum of game-round points for the active meeting.
     *
     * @param color team color identifier (case-insensitive)
     * @return map with keys "members", "individualScore", "gameScore", "totalScore"
     */
    @GetMapping("/status/{color}")
    public Map<String, Object> getTeamStatus(@PathVariable String color) {
        Map<String, Object> response = new HashMap<>();
        Meeting activeMeeting = getActiveMeeting();

        List<Child> members = childRepository.findByCurrentTeamIgnoreCase(color);

        int individualSum = members.stream()
                .mapToInt(c -> c.getDailyPoints() != null ? c.getDailyPoints() : 0)
                .sum();

        int gameSum = 0;
        if (activeMeeting != null) {
            List<TeamGamePoint> gamePoints = teamGamePointRepository
                    .findByMeetingIdAndTeamColor(activeMeeting.getId(), color);
            gameSum = gamePoints.stream().mapToInt(TeamGamePoint::getPoints).sum();
        }

        response.put("members", members);
        response.put("individualScore", individualSum);
        response.put("gameScore", gameSum);
        response.put("totalScore", individualSum + gameSum);

        return response;
    }

    /**
     * Assigns a child to a team.
     *
     * @param payload JSON with keys "childId" (string) and "teamColor" (string)
     * @return 200 on success; 400 on invalid child ID or parse error
     */
    @PostMapping("/pick")
    public ResponseEntity<?> pickChild(@RequestBody Map<String, String> payload) {
        try {
            Integer childId = Integer.parseInt(payload.get("childId"));
            String teamColor = payload.get("teamColor");

            Child child = childRepository.findById(childId).orElse(null);
            if (child == null) return ResponseEntity.badRequest().body("Invalid child.");

            child.setCurrentTeam(teamColor);
            childRepository.save(child);

            return ResponseEntity.ok("Added to " + teamColor);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Records the results of a game round. Saves one {@link TeamGamePoint} entry per team.
     * Points are based on finishing position (1st: 1000, 2nd: 500, 3rd: 300, 4th: 100)
     * and are doubled if the round is marked as a double-points round.
     *
     * @param payload JSON with "ranking" (ordered list of team colors, winner first) and "isDouble" (boolean)
     * @return 200 on success; 400 if no active session exists
     */
    @PostMapping("/game-round")
    public ResponseEntity<?> saveGameRound(@RequestBody Map<String, Object> payload) {
        Meeting activeMeeting = getActiveMeeting();
        if (activeMeeting == null) return ResponseEntity.badRequest().body("No active session exists.");

        @SuppressWarnings("unchecked")
        List<String> ranking = (List<String>) payload.get("ranking");
        Boolean isDouble = (Boolean) payload.get("isDouble");

        int[] standardPoints = {1000, 500, 300, 100};

        for (int i = 0; i < ranking.size(); i++) {
            String color = ranking.get(i);
            int pts = (i < standardPoints.length) ? standardPoints[i] : 0;
            if (Boolean.TRUE.equals(isDouble)) pts *= 2;

            TeamGamePoint tp = new TeamGamePoint();
            tp.setMeeting(activeMeeting);
            tp.setTeamColor(color);
            tp.setPoints(pts);
            teamGamePointRepository.save(tp);
        }

        return ResponseEntity.ok("Game round saved.");
    }

    /**
     * Adds a manually specified number of points to a team without a game-round ranking.
     * Useful for bonus points or corrections by the director.
     *
     * @param payload JSON with "teamColor" (string) and "points" (integer)
     * @return 200 on success; 400 if team color or points are missing
     */
    @PostMapping("/add-manual-points")
    public ResponseEntity<?> addManualPoints(@RequestBody Map<String, Object> payload) {
        String color = (String) payload.get("teamColor");
        Integer points = (Integer) payload.get("points");

        if (color == null || points == null) {
            return ResponseEntity.badRequest().body("Team color and points are required.");
        }

        TeamGamePoint log = new TeamGamePoint();
        log.setTeamColor(color.toLowerCase());
        log.setPoints(points);
        log.setMeeting(getActiveMeeting());
        teamGamePointRepository.save(log);

        return ResponseEntity.ok("Added " + points + " points to team " + color.toUpperCase());
    }

    /**
     * Returns the first open meeting ordered by date, or {@code null} if none exists.
     */
    private Meeting getActiveMeeting() {
        return meetingRepository.findByIsCompletedFalseOrderByDateAsc()
                .stream().findFirst().orElse(null);
    }
}
