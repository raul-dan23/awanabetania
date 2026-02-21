package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.DataInitializer; // Import pentru username
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.ChildManual;
import com.awanabetania.awanabetania.Model.ChildProgress;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/children")
@CrossOrigin(origins = "*")
public class ChildController {

    @Autowired private ChildRepository childRepository;
    @Autowired private ChildManualRepository childManualRepository;
    @Autowired private ChildProgressRepository childProgressRepository;
    @Autowired private NotificationRepository notificationRepository;

    // --- IMPORTANTE PENTRU STERGERE ---
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private WarningRepository warningRepository;

    @GetMapping
    public List<Child> getAllChildren() { return childRepository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Child> getChildById(@PathVariable Integer id) {
        return childRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/add")
    public Child addChild(@RequestBody Child child) {
        // Generam automat username-ul la adaugarea manuala a unui copil
        String baseUsername = DataInitializer.generateCleanUsername(child.getName(), child.getSurname());
        if (childRepository.findByUsername(baseUsername).isPresent()) {
            baseUsername += new java.util.Random().nextInt(1000);
        }
        child.setUsername(baseUsername);

        return childRepository.save(child);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateChild(@PathVariable Integer id, @RequestBody Child childDetails) {
        return childRepository.findById(id).map(child -> {

            // --- START VALIDARE USERNAME ---
            // Verificăm dacă noul username există deja la alt copil (id diferit)
            var existingUser = childRepository.findByUsername(childDetails.getUsername());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body("❌ Acest username este deja folosit de altcineva!");
            }
            // --- END VALIDARE USERNAME ---

            child.setName(childDetails.getName());
            child.setSurname(childDetails.getSurname());
            child.setUsername(childDetails.getUsername()); // Salvăm noul username
            child.setBirthDate(childDetails.getBirthDate());
            child.setParentName(childDetails.getParentName());
            child.setParentPhone(childDetails.getParentPhone());

            return ResponseEntity.ok(childRepository.save(child));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{childId}/unlock-next")
    @Transactional
    public ResponseEntity<?> unlockNextSticker(@PathVariable Integer childId) {
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();

        // 1. Gestionam progresul (Stickerul)
        ChildProgress progress = child.getProgress();
        if (progress == null) {
            progress = new ChildProgress();
            progress.setChild(child);
            progress.setLastStickerId(0);
            progress.setManualsCount(0);
        }

        int nextId = progress.getLastStickerId() + 1;
        progress.setLastStickerId(nextId);

        // 2. Incrementam numarul de lectii completate
        int currentLessons = child.getLessonsCompleted() != null ? child.getLessonsCompleted() : 0;
        child.setLessonsCompleted(currentLessons + 1);

        // 3. Dam puncte (20 pct per sticker)
        int pts = child.getSeasonPoints() != null ? child.getSeasonPoints() : 0;
        child.setSeasonPoints(pts + 20);

        String message = "Sticker " + nextId + " deblocat!";

        if (nextId % 20 == 0) {
            createNotification(child, "MANUAL_FINISHED", "🎉 DIRECTOR! " + child.getName() + " a terminat manualul!");
            message += " | 🏁 Manual Terminat!";
        }

        childRepository.save(child);
        childProgressRepository.save(progress);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/{childId}/give-reward")
    @Transactional
    public ResponseEntity<?> giveReward(@PathVariable Integer childId, @RequestParam String type) {
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();
        if ("SHIRT".equalsIgnoreCase(type)) {
            child.setHasShirt(true);
            List<Notification> notifs = notificationRepository.findActiveByChildAndType(childId, "SHIRT_ELIGIBLE");
            for(Notification n : notifs) { n.setIsVisible(false); notificationRepository.save(n); }
            childRepository.save(child);
            return ResponseEntity.ok("✅ Tricou acordat!");
        }
        else if ("HAT".equalsIgnoreCase(type)) {
            child.setHasHat(true);
            List<Notification> notifs = notificationRepository.findActiveByChildAndType(childId, "HAT_ELIGIBLE");
            for(Notification n : notifs) { n.setIsVisible(false); notificationRepository.save(n); }
            childRepository.save(child);
            return ResponseEntity.ok("✅ Căciulă acordată!");
        }
        return ResponseEntity.badRequest().body("Invalid type");
    }

    @PostMapping("/{childId}/assign-manual")
    @Transactional
    public ResponseEntity<?> assignManual(@PathVariable Integer childId, @RequestParam String manualName) {
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();
        ChildManual cm = new ChildManual();
        cm.setName(manualName); cm.setStatus("ACTIVE"); cm.setStartDate(LocalDate.now()); cm.setChild(child);
        childManualRepository.save(cm);
        ChildProgress cp = child.getProgress();
        if(cp == null) { cp = new ChildProgress(); cp.setChild(child); cp.setLastStickerId(0); cp.setManualsCount(0); }
        cp.setManualsCount(cp.getManualsCount() + 1);
        childProgressRepository.save(cp);
        child.setHasManual(true);
        childRepository.save(child);
        return ResponseEntity.ok("Manual atribuit!");
    }

    private void createNotification(Child child, String type, String msg) {
        Notification n = new Notification();
        n.setMessage(msg); n.setType(type); n.setVisibleTo("DIRECTOR"); n.setDate(LocalDate.now()); n.setIsVisible(true); n.setChildId(child.getId());
        notificationRepository.save(n);
    }

    /**
     * STERGERE COPIL CU COD DE SECURITATE
     * Endpoint: DELETE /api/children/{id}?code=A1B2
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteChild(@PathVariable Integer id, @RequestParam(required = false) String code) {

        Child child = childRepository.findById(id).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();

        // --- VERIFICARE COD ---
        if (child.getDeletionCode() != null && !child.getDeletionCode().isEmpty()) {
            if (code == null || !code.equalsIgnoreCase(child.getDeletionCode())) {
                return ResponseEntity.badRequest().body("❌ Codul de ștergere este incorect!");
            }
        }

        // --- LOGICA DE CURATENIE (Aceeasi ca inainte) ---
        scoreRepository.deleteByChildId(id);
        notificationRepository.deleteByChildId(id);
        childManualRepository.deleteByChildId(id);
        childProgressRepository.deleteByChildId(id);
        warningRepository.deleteByChildId(id);

        childRepository.deleteById(id);

        return ResponseEntity.ok("✅ Copilul și toate datele au fost șterse!");
    }
}