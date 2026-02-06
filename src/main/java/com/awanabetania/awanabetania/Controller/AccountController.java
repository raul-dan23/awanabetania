package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {

    @Autowired private LeaderRepository leaderRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private NotificationRepository notificationRepository;

    /**
     * PASUL 1: Solicitare Ștergere Cont.
     * Generează un cod și trimite notificare la Director.
     */
    @PostMapping("/request-deletion")
    public ResponseEntity<?> requestDeletion(@RequestBody Map<String, Object> payload) {
        Integer id = (Integer) payload.get("id");
        String role = (String) payload.get("role");

        if ("LEADER".equalsIgnoreCase(role) && id == 1) {
            return ResponseEntity.badRequest().body("Administratorul nu poate fi șters!");
        }

        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String userName = "";

        if ("CHILD".equalsIgnoreCase(role)) {
            Child c = childRepository.findById(id).orElse(null);
            if (c == null) return ResponseEntity.badRequest().body("Utilizator inexistent");
            c.setDeletionCode(code);
            childRepository.save(c);
            userName = c.getName() + " " + c.getSurname() + " (Copil)";
        } else {
            Leader l = leaderRepository.findById(id).orElse(null);
            if (l == null) return ResponseEntity.badRequest().body("Utilizator inexistent");
            l.setDeletionCode(code);
            leaderRepository.save(l);
            userName = l.getName() + " " + l.getSurname() + " (Lider)";
        }

        // Notificare Director
        String adminMsg = String.format("🗑️ SOLICITARE: %s vrea să șteargă contul. Cod: %s.", userName, code);
        Notification n = new Notification(adminMsg, "ALERT", "1", LocalDate.now());
        notificationRepository.save(n);

        return ResponseEntity.ok("Solicitare trimisă! Așteaptă codul de la Director.");
    }
}