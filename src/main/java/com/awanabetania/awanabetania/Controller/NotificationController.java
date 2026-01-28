package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Returnează notificările active pentru un anumit lider + cele publice ("ALL").
     * Folosește metoda 'findMyActiveNotifications' definită în Repository.
     */
    @GetMapping
    public List<Notification> getMyNotifications(@RequestParam String leaderId) {
        // Apelăm metoda cu parametrii String, String
        return notificationRepository.findMyActiveNotifications(leaderId, "ALL");
    }

    /**
     * Adaugă o notificare manuală (dacă e nevoie vreodată din Postman/Frontend).
     */
    @PostMapping("/add")
    public Notification addNotification(@RequestBody Notification notification) {
        notification.setDate(LocalDate.now());
        notification.setIsVisible(true);
        return notificationRepository.save(notification);
    }

    /**
     * Ștergere logică (Soft Delete).
     * Când apeși X în Dashboard, se apelează asta.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Integer id) {
        return notificationRepository.findById(id)
                .map(notification -> {
                    notification.setIsVisible(false); // Nu o ștergem fizic, doar o ascundem
                    notificationRepository.save(notification);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}