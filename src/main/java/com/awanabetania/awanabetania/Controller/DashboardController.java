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

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired private ChildRepository childRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private NotificationRepository notificationRepository;

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats(@RequestParam(required = false) Integer leaderId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("clubName", "Awana Betania");
        stats.put("kidsCount", childRepository.count());
        stats.put("leadersCount", leaderRepository.count());

        List<Leader> directors = leaderRepository.findAll().stream()
                .filter(l -> l.getRole() != null && (l.getRole().equalsIgnoreCase("DIRECTOR") || l.getRole().equalsIgnoreCase("COORDONATOR")))
                .collect(Collectors.toList());
        stats.put("directors", directors);

        if (leaderId != null) {
            Leader currentLeader = leaderRepository.findById(leaderId).orElse(null);

            // 1. Notificari publice (ALL)
            List<Notification> publicN = notificationRepository.findByVisibleTo("ALL");

            // 2. Notificari personale (ID)
            List<Notification> personalN = notificationRepository.findByVisibleTo(String.valueOf(leaderId));

            // 3. Notificari Director (doar daca are rolul)
            List<Notification> directorN = new ArrayList<>();
            if (currentLeader != null &&
                    (currentLeader.getRole().equalsIgnoreCase("DIRECTOR") || currentLeader.getRole().equalsIgnoreCase("COORDONATOR"))) {
                directorN = notificationRepository.findByVisibleTo("DIRECTOR");
            }

            // Combinare si sortare
            List<Notification> finalN = Stream.of(publicN, personalN, directorN)
                    .flatMap(Collection::stream)
                    .distinct()
                    .sorted(Comparator.comparing(Notification::getId).reversed())
                    .limit(20)
                    .collect(Collectors.toList());

            stats.put("notifications", finalN);
        } else {
            Notification w = new Notification(); w.setId(0); w.setMessage("Bine ai venit!"); w.setDate(LocalDate.now());
            stats.put("notifications", List.of(w));
        }

        stats.put("reminders", List.of("📅 18:00 - Incepere", "📅 19:30 - Premierea"));
        return stats;
    }
}