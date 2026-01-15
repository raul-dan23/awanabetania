package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.*;
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

    // GET: Aduce doar notificarile active (ne-sterse)
    @GetMapping
    public List<Notification> getAll() {
        // Acum metoda aceasta exista in Repository si nu mai da eroare
        return notificationRepository.findByIsVisibleTrueOrderByIdDesc();
    }

    // GET pentru un lider specific (Optional, daca folosesti filtrarea)
    @GetMapping("/{leaderId}")
    public List<Notification> getForLeader(@PathVariable String leaderId) {
        return notificationRepository.findMyActiveNotifications(leaderId, "ALL");
    }

    // POST: Adauga o notificare noua
    @PostMapping("/add")
    public Notification add(@RequestBody Notification n) {
        // AICI ERA EROAREA: Am schimbat LocalDateTime in LocalDate
        n.setDate(LocalDate.now());
        n.setIsVisible(true);
        return notificationRepository.save(n);
    }

    // DELETE: "Stergere logica" - O ascundem
    @DeleteMapping("/{id}")
    public ResponseEntity<?> hideNotification(@PathVariable Integer id) {
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n == null) return ResponseEntity.badRequest().body("Nu exista.");

        n.setIsVisible(false); // O ascundem
        notificationRepository.save(n); // Salvam modificarea

        return ResponseEntity.ok("Notificare ștearsă (ascunsă)!");
    }
}