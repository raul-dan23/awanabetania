package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Dto.DirectorContactResponse;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import com.awanabetania.awanabetania.Security.AuthUser;
import com.awanabetania.awanabetania.Service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides aggregate statistics and notifications for the dashboard screen.
 * When a {@code leaderId} is supplied, notifications are assembled from three sources:
 * public announcements ("ALL"), personal notifications (the leader's own ID), and
 * director-level alerts (if the leader's role is DIRECTOR or COORDONATOR).
 * Results are deduplicated, sorted newest-first, and capped at 20 entries.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private ChildRepository childRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private SeasonService seasonService;

    /**
     * Returns dashboard statistics and the notification feed for the requesting leader.
     *
     * @param leaderId when present, the notification feed is included. Whose feed it is comes
     *                 from the token, never from this value: child and leader ids overlap, so a
     *                 child whose id matched the director's used to receive director alerts
     * @return map with keys: "clubName", "season" (the current season's name), "kidsCount",
     *         "leadersCount", "directors", "notifications", "reminders"
     */
    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats(@RequestParam(required = false) Integer leaderId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("clubName", "Awana Betania");
        stats.put("season", seasonService.current().getName());
        stats.put("kidsCount", childRepository.count());
        stats.put("leadersCount", leaderRepository.count());

        List<DirectorContactResponse> directors = leaderRepository.findByRoleIgnoreCaseIn(List.of("director", "coordonator"))
                .stream().map(DirectorContactResponse::from).toList();
        stats.put("directors", directors);

        AuthUser me = AuthUser.current();
        if (leaderId != null && me != null) {
            List<Notification> publicN = notificationRepository.findByVisibleTo("ALL");
            List<Notification> personalN = me.isLeader()
                    ? notificationRepository.findByVisibleTo(String.valueOf(me.id()))
                    : List.of();
            List<Notification> directorN = me.isDirector()
                    ? notificationRepository.findByVisibleTo("DIRECTOR")
                    : List.of();

            // Merge, deduplicate, sort newest-first, and cap at 20
            List<Notification> finalN = Stream.of(publicN, personalN, directorN)
                    .flatMap(Collection::stream)
                    .distinct()
                    .sorted(Comparator.comparing(Notification::getId).reversed())
                    .limit(20)
                    .collect(Collectors.toList());

            stats.put("notifications", finalN);
        } else {
            Notification welcome = new Notification();
            welcome.setId(0);
            welcome.setMessage("Welcome!");
            welcome.setDate(LocalDate.now());
            stats.put("notifications", List.of(welcome));
        }

        stats.put("reminders", List.of("18:00 - Start", "19:30 - Awards"));
        return stats;
    }
}
