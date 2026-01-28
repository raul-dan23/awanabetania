package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.ChildManual;
import com.awanabetania.awanabetania.Model.ChildProgress;
import com.awanabetania.awanabetania.Model.Notification;
import com.awanabetania.awanabetania.Repository.*; // Importam toate repo-urile
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
    public Child addChild(@RequestBody Child child) { return childRepository.save(child); }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateChild(@PathVariable Integer id, @RequestBody Child childDetails) {
        Child child = childRepository.findById(id).orElse(null);
        if(child == null) return ResponseEntity.notFound().build();

        child.setName(childDetails.getName());
        child.setSurname(childDetails.getSurname());
        child.setParentName(childDetails.getParentName());
        child.setParentPhone(childDetails.getParentPhone());
        child.setBirthDate(childDetails.getBirthDate());
        child.setIsSuspended(childDetails.getIsSuspended());

        return ResponseEntity.ok(childRepository.save(child));
    }

    // ... (Metodele unlock-next, give-reward, assign-manual raman neschimbate cum ti le-am dat ultima data) ...
    // Le pun pe scurt aici ca sa fie fisierul complet, dar tu ai deja codul bun pentru ele.

    @PostMapping("/{childId}/unlock-next")
    @Transactional
    public ResponseEntity<?> unlockNextSticker(@PathVariable Integer childId) {
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();
        ChildProgress progress = child.getProgress();
        if (progress == null) { progress = new ChildProgress(); progress.setChild(child); progress.setLastStickerId(0); progress.setManualsCount(0); }

        int nextId = progress.getLastStickerId() + 1;
        progress.setLastStickerId(nextId);

        int currentLessons = child.getLessonsCompleted() != null ? child.getLessonsCompleted() : 0;
        int newLessonCount = currentLessons + 1;
        child.setLessonsCompleted(newLessonCount);
        int pts = child.getSeasonPoints() != null ? child.getSeasonPoints() : 0;
        child.setSeasonPoints(pts + 20);

        String message = "Sticker " + nextId + " deblocat!";
        if (newLessonCount % 3 == 0) {
            int currentBadges = child.getBadgesCount() != null ? child.getBadgesCount() : 0;
            child.setBadgesCount(currentBadges + 1);
            message += " | 🏅 INSIGNĂ NOUĂ!";
        }
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
     * --- METODA NOUA DE STERGERE (CASCADE DELETE) ---
     * Sterge absolut tot ce tine de copil inainte sa stearga copilul.
     */
    @DeleteMapping("/{id}")
    @Transactional // Obligatoriu pentru stergeri multiple!
    public ResponseEntity<?> deleteChild(@PathVariable Integer id) {
        if (!childRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // 1. Stergem Istoricul de Puncte
        scoreRepository.deleteByChildId(id);

        // 2. Stergem Notificarile
        notificationRepository.deleteByChildId(id);

        // 3. Stergem Manualele
        childManualRepository.deleteByChildId(id);

        // 4. Stergem Progresul (Stickere)
        childProgressRepository.deleteByChildId(id);

        // 5. Stergem Pedepsele
        warningRepository.deleteByChildId(id);

        // 6. LA FINAL: Stergem copilul
        childRepository.deleteById(id);

        return ResponseEntity.ok("✅ Copilul și toate datele aferente au fost șterse definitiv!");
    }
}