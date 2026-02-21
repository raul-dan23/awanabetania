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
        String baseUsername = DataInitializer.generateCleanUsername(child.getName(), child.getSurname());
        if (childRepository.findByUsername(baseUsername).isPresent()) {
            baseUsername += new java.util.Random().nextInt(1000);
        }
        child.setUsername(baseUsername);
        return childRepository.save(child);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateChild(@PathVariable Integer id, @RequestBody Child childDetails) {
        return childRepository.findById(id).map(child -> {
            var existingUser = childRepository.findByUsername(childDetails.getUsername());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body("❌ Acest username este deja folosit de altcineva!");
            }

            child.setName(childDetails.getName());
            child.setSurname(childDetails.getSurname());
            child.setUsername(childDetails.getUsername());
            child.setBirthDate(childDetails.getBirthDate());
            child.setParentName(childDetails.getParentName());
            child.setParentPhone(childDetails.getParentPhone());

            return ResponseEntity.ok(childRepository.save(child));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteChild(@PathVariable Integer id, @RequestParam(required = false) String code) {
        Child child = childRepository.findById(id).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();

        if (child.getDeletionCode() != null && !child.getDeletionCode().isEmpty()) {
            if (code == null || !code.equalsIgnoreCase(child.getDeletionCode())) {
                return ResponseEntity.badRequest().body("❌ Codul de ștergere este incorect!");
            }
        }

        // Datorită CascadeType.ALL din Model, multe ștergeri se fac acum automat.
        // Păstrăm totuși curățarea manuală pentru siguranță acolo unde nu există relații directe @OneToMany.
        scoreRepository.deleteByChildId(id);
        notificationRepository.deleteByChildId(id);
        warningRepository.deleteByChildId(id);

        childRepository.deleteById(id);
        return ResponseEntity.ok("✅ Cont șters cu succes!");
    }

    // ... restul metodelor raman neschimbate
}