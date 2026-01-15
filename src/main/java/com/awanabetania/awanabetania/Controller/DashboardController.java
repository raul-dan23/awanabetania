package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aceasta clasa pregateste datele pentru prima pagina (Dashboard).
 * Cand deschizi aplicatia, acest cod ruleaza ca sa iti arate statistici si noutati.
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    // Avem nevoie de acces la tabele pentru a numara copiii, liderii si mesajele
    @Autowired private ChildRepository childRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private NotificationRepository notificationRepository;

    /**
     * Aceasta metoda strange toate informatiile necesare pentru ecranul principal.
     * Primeste ID-ul liderului ca sa stim ce mesaje sa ii aratam doar lui.
     */
    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats(@RequestParam(required = false) Integer leaderId) {
        Map<String, Object> stats = new HashMap<>();

        // Punem in pachet numele clubului si cati oameni sunt inscrisi in total
        stats.put("clubName", "Awana Betania");
        stats.put("kidsCount", childRepository.count());
        stats.put("leadersCount", leaderRepository.count());

        // Cautam in lista cine sunt sefii (Directori sau Coordonatori) ca sa ii afisam separat
        List<Leader> directors = leaderRepository.findAll().stream()
                .filter(l -> l.getRole() != null &&
                        (l.getRole().equalsIgnoreCase("DIRECTOR") || l.getRole().equalsIgnoreCase("COORDONATOR")))
                .collect(Collectors.toList());
        stats.put("directors", directors);

        // Aici ne ocupam de notificari (clopotelul de sus)
        if (leaderId != null) {
            // Luam mesajele private (pentru el) sau cele publice (pentru toata lumea - ALL)
            List<Notification> notifications = notificationRepository.findByVisibleToOrVisibleToOrderByIdDesc(String.valueOf(leaderId), "ALL");

            // Pastram doar textul mesajului si luam doar ultimele 5 (cele mai noi)
            List<String> notifMessages = notifications.stream()
                    .map(Notification::getMessage)
                    .limit(5)
                    .collect(Collectors.toList());

            stats.put("notifications", notifMessages);
        } else {
            // Daca nu e nimeni logat, afisam un mesaj standard de bun venit
            stats.put("notifications", List.of("Bine ai venit!"));
        }

        // Aici punem orarul serii (momentan este fix, scris de mana)
        stats.put("reminders", List.of("📅 18:00 - Incepere", "📅 19:30 - Premierea"));

        return stats;
    }
}