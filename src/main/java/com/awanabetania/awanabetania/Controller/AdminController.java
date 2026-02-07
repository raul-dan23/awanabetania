package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Model.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private LeaderRepository leaderRepository;

    @Autowired
    private ChildRepository childRepository;

    // 1. OBTINE TOTI UTILIZATORII (Lideri + Copii) pentru tabel
    @GetMapping("/all-users")
    public ResponseEntity<?> getAllUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("leaders", leaderRepository.findAll());
        response.put("children", childRepository.findAll());
        return ResponseEntity.ok(response);
    }

    // 2. DECRIPTARE PAROLA (The Magic Eye 👁️)
    // Primeste un text criptat (ex: "Xy7z...") si returneaza parola reala ("1234")
    @PostMapping("/decrypt-password")
    public ResponseEntity<?> decryptPassword(@RequestBody Map<String, String> payload) {
        String encryptedPass = payload.get("password");

        if (encryptedPass == null || encryptedPass.isEmpty()) {
            return ResponseEntity.badRequest().body("Lipsa parola criptata");
        }

        try {
            // Folosim cheia noastra secreta ca sa aflam adevarul
            String realPassword = AESUtil.decrypt(encryptedPass);
            return ResponseEntity.ok(Collections.singletonMap("realPassword", realPassword));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Eroare la decriptare");
        }
    }
}