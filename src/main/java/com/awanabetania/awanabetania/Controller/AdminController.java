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

    // 2. DECRIPTARE PAROLA — necesita PIN in header
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