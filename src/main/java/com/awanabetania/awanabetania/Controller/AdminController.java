package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.AESUtil;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides privileged admin operations protected by the admin PIN ({@code X-Admin-Pin} header).
 * Exposes:
 * <ul>
 *   <li>PIN verification</li>
 *   <li>Full user listing</li>
 *   <li>NFC card association and removal per child</li>
 *   <li>Password decryption for the admin panel</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private LeaderRepository leaderRepository;
    @Autowired private ChildRepository childRepository;

    @Value("${admin.pin}")
    private String adminPin;

    /** Returns {@code true} if the supplied PIN matches the configured admin PIN. */
    private boolean isPinValid(String pin) {
        return adminPin != null && adminPin.equals(pin);
    }

    /**
     * Verifies the admin PIN. Called by the frontend before displaying the admin panel.
     *
     * @param payload JSON with key "pin"
     * @return 200 "OK" on match; 401 on mismatch
     */
    @PostMapping("/verify-pin")
    public ResponseEntity<?> verifyPin(@RequestBody Map<String, String> payload) {
        if (isPinValid(payload.get("pin"))) return ResponseEntity.ok("OK");
        return ResponseEntity.status(401).body("Incorrect PIN");
    }

    /**
     * Returns all leaders and children. Intended for the admin Control Center.
     *
     * @param pin admin PIN from the {@code X-Admin-Pin} header (optional but required to succeed)
     * @return 200 with maps "leaders" and "children"; 401 on invalid PIN
     */
    @GetMapping("/all-users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Unauthorized");
        Map<String, Object> response = new HashMap<>();
        response.put("leaders", leaderRepository.findAll());
        response.put("children", childRepository.findAll());
        return ResponseEntity.ok(response);
    }

    /**
     * Associates an NFC card UID with a child from the admin Control Center.
     * If the UID is already registered to a different child, that binding is cleared first.
     *
     * @param pin  admin PIN from the {@code X-Admin-Pin} header
     * @param body JSON with "childId" (Integer) and "uid" (String)
     * @return 200 with child info on success; 401/400/404 on error
     */
    @PostMapping("/nfc-register")
    public ResponseEntity<?> adminRegisterNfc(
            @RequestHeader("X-Admin-Pin") String pin,
            @RequestBody Map<String, Object> body) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Incorrect PIN");

        Integer childId = (Integer) body.get("childId");
        String uid = (String) body.get("uid");
        if (childId == null || uid == null || uid.isBlank())
            return ResponseEntity.badRequest().body("childId and uid are required.");

        childRepository.findByNfcUid(uid).ifPresent(existing -> {
            if (!existing.getId().equals(childId)) {
                existing.setNfcUid(null);
                childRepository.save(existing);
            }
        });

        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();

        child.setNfcUid(uid);
        childRepository.save(child);

        return ResponseEntity.ok(Map.of(
                "message", "Card associated.",
                "childId", child.getId(),
                "name", child.getName() + " " + child.getSurname(),
                "uid", uid
        ));
    }

    /**
     * Removes the NFC card association from a child.
     *
     * @param childId the child's primary key
     * @param pin     admin PIN from the {@code X-Admin-Pin} header
     * @return 200 on success; 401 on invalid PIN; 404 if child not found
     */
    @DeleteMapping("/nfc-remove/{childId}")
    public ResponseEntity<?> adminRemoveNfc(
            @PathVariable Integer childId,
            @RequestHeader("X-Admin-Pin") String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Incorrect PIN");

        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();

        child.setNfcUid(null);
        childRepository.save(child);

        return ResponseEntity.ok(Map.of("message", "Card dissociated."));
    }

    /**
     * Decrypts a Base64-encoded AES-encrypted password back to plain text.
     * Intended for the admin panel to display legacy passwords.
     *
     * @param pin     admin PIN from the {@code X-Admin-Pin} header (optional but required to succeed)
     * @param payload JSON with key "password" (the encrypted value)
     * @return 200 with "realPassword" on success; 401 on invalid PIN; 400/500 on decryption error
     */
    @PostMapping("/decrypt-password")
    public ResponseEntity<?> decryptPassword(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @RequestBody Map<String, String> payload) {
        if (!isPinValid(pin)) return ResponseEntity.status(401).body("Unauthorized");

        String encryptedPass = payload.get("password");
        if (encryptedPass == null || encryptedPass.isEmpty()) {
            return ResponseEntity.badRequest().body("No encrypted password provided.");
        }

        try {
            String realPassword = AESUtil.decrypt(encryptedPass);
            return ResponseEntity.ok(Collections.singletonMap("realPassword", realPassword));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Decryption error.");
        }
    }
}
