package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Model.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private LeaderRepository leaderRepository;
    @Autowired private ChildRepository childRepository;

    @Value("${admin.pin}")
    private String adminPin;

    private boolean isPinValid(String pin) {
        return adminPin != null && adminPin.equals(pin);
    }

    // 0. VERIFICA PIN — frontend apeleaza asta inainte de orice
    @PostMapping("/verify-pin")
    public ResponseEntity<?> verifyPin(@RequestBody Map<String, String> payload) {
        if (isPinValid(payload.get("pin"))) return ResponseEntity.ok("OK");
        return ResponseEntity.status(401).body("PIN incorect");
    }

    // 1. TOTI UTILIZATORII — necesita PIN in header
    @GetMapping("/all-users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Acces neautorizat");
        Map<String, Object> response = new HashMap<>();
        response.put("leaders", leaderRepository.findAll());
        response.put("children", childRepository.findAll());
        return ResponseEntity.ok(response);
    }

    // 2. ASOCIAZA CARD NFC — necesita PIN in header
    @PostMapping("/nfc-register")
    public ResponseEntity<?> adminRegisterNfc(
            @RequestHeader("X-Admin-Pin") String pin,
            @RequestBody Map<String, Object> body) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("PIN incorect");
        Integer childId = (Integer) body.get("childId");
        String uid = (String) body.get("uid");
        if (childId == null || uid == null || uid.isBlank())
            return ResponseEntity.badRequest().body("childId si uid sunt obligatorii.");
        childRepository.findByNfcUid(uid).ifPresent(existing -> {
            if (!existing.getId().equals(childId)) { existing.setNfcUid(null); childRepository.save(existing); }
        });
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();
        child.setNfcUid(uid);
        childRepository.save(child);
        return ResponseEntity.ok(Map.of("message", "Card asociat.", "childId", child.getId(),
                "name", child.getName() + " " + child.getSurname(), "uid", uid));
    }

    // 3. STERGE CARD NFC — necesita PIN in header
    @DeleteMapping("/nfc-remove/{childId}")
    public ResponseEntity<?> adminRemoveNfc(
            @PathVariable Integer childId,
            @RequestHeader("X-Admin-Pin") String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("PIN incorect");
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();
        child.setNfcUid(null);
        childRepository.save(child);
        return ResponseEntity.ok(Map.of("message", "Card dezasociat."));
    }

    // 4. DECRIPTARE PAROLA — necesita PIN in header
    @PostMapping("/decrypt-password")
    public ResponseEntity<?> decryptPassword(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @RequestBody Map<String, String> payload) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Acces neautorizat");
        String encryptedPass = payload.get("password");
        if (encryptedPass == null || encryptedPass.isEmpty()) {
            return ResponseEntity.badRequest().body("Lipsa parola criptata");
        }
        try {
            String realPassword = AESUtil.decrypt(encryptedPass);
            return ResponseEntity.ok(Collections.singletonMap("realPassword", realPassword));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Eroare la decriptare");
        }
    }
}