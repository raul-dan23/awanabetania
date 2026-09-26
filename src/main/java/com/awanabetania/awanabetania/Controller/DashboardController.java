package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
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

    /**
     * Returns dashboard statistics and the notification feed for the requesting leader.
     *
     * @param leaderId optional ID of the currently logged-in leader;
     *                 if absent, only a welcome message is returned
     * @return map with keys: "clubName", "kidsCount", "leadersCount", "directors",
     *         "notifications", "reminders"
     */
    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats(@RequestParam(required = false) Integer leaderId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("clubName", "Awana Betania");
        stats.put("kidsCount", childRepository.count());
        stats.put("leadersCount", leaderRepository.count());

        List<Leader> directors = leaderRepository.findByRoleIgnoreCaseIn(List.of("director", "coordonator"));
        stats.put("directors", directors);

        if (leaderId != null) {
            Leader currentLeader = leaderRepository.findById(leaderId).orElse(null);

            List<Notification> publicN = notificationRepository.findByVisibleTo("ALL");
            List<Notification> personalN = notificationRepository.findByVisibleTo(String.valueOf(leaderId));
            List<Notification> directorN = new ArrayList<>();

            if (currentLeader != null &&
                    (currentLeader.getRole().equalsIgnoreCase("DIRECTOR") ||
                     currentLeader.getRole().equalsIgnoreCase("COORDONATOR"))) {
                directorN = notificationRepository.findByVisibleTo("DIRECTOR");
            }

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
