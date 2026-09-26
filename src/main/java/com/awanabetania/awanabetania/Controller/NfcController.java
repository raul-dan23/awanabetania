package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles NFC card operations called by the local NFC bridge (nfc-bridge.jar).
 * All endpoints require the {@code X-NFC-Token} header to match the configured token.
 *
 * <ul>
 *   <li>Register: associates a card UID with a child; if the UID is already bound to
 *       a different child, that binding is silently cleared first.</li>
 *   <li>Lookup: retrieves a child's name and current point balance by UID.</li>
 *   <li>Spend: deducts a specified point amount from the child's season balance.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/nfc")
public class NfcController {

    @Autowired
    private ChildRepository childRepository;

    @Value("${nfc.token}")
    private String nfcToken;

    /** Returns {@code true} if the supplied token matches the configured NFC token. */
    private boolean isAuthorized(String token) {
        return nfcToken != null && nfcToken.equals(token);
    }

    /**
     * Associates a physical NFC card UID with a child.
     * If the UID is already registered to a different child, that child's UID is cleared first
     * so the UID remains unique.
     *
     * @param token  NFC token from the {@code X-NFC-Token} header
     * @param body   JSON with "childId" (Integer) and "uid" (String)
     * @return 200 with child info on success; 403 on invalid token; 400/404 on bad data
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerCard(
            @RequestHeader("X-NFC-Token") String token,
            @RequestBody Map<String, Object> body) {

        if (!isAuthorized(token)) return ResponseEntity.status(403).body("Invalid token.");

        Integer childId = (Integer) body.get("childId");
        String uid = (String) body.get("uid");
        if (childId == null || uid == null || uid.isBlank())
            return ResponseEntity.badRequest().body("childId and uid are required.");

        // Clear any existing binding for this UID before reassigning
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
                "message", "Card registered successfully.",
                "childId", child.getId(),
                "name", child.getName() + " " + child.getSurname(),
                "uid", uid
        ));
    }

    /**
     * Returns a child's basic info and current point balance by NFC card UID.
     * Called by the NFC bridge after each card scan at the fair.
     *
     * @param uid   the card's hardware UID from the path
     * @param token NFC token from the {@code X-NFC-Token} header
     * @return 200 with child data; 403 on invalid token; 404 if the UID is not registered
     */
    @GetMapping("/{uid}")
    public ResponseEntity<?> getChildByUid(
            @PathVariable String uid,
            @RequestHeader("X-NFC-Token") String token) {

        if (!isAuthorized(token)) return ResponseEntity.status(403).body("Invalid token.");

        Child child = childRepository.findByNfcUid(uid).orElse(null);
        if (child == null) return ResponseEntity.status(404).body("Unknown card.");

        return ResponseEntity.ok(Map.of(
                "id", child.getId(),
                "name", child.getName(),
                "surname", child.getSurname(),
                "seasonPoints", child.getSeasonPoints() != null ? child.getSeasonPoints() : 0,
                "uid", uid
        ));
    }

    /**
     * Deducts a specified point amount from a child's season balance.
     * Rejects the operation if the child does not have enough points.
     *
     * @param uid   the card's hardware UID from the path
     * @param token NFC token from the {@code X-NFC-Token} header
     * @param body  JSON with "amount" (int)
     * @return 200 with remaining balance on success; 403/404/400 on error
     */
    @PostMapping("/{uid}/spend")
    public ResponseEntity<?> spendPoints(
            @PathVariable String uid,
            @RequestHeader("X-NFC-Token") String token,
            @RequestBody Map<String, Object> body) {

        if (!isAuthorized(token)) return ResponseEntity.status(403).body("Invalid token.");

        Child child = childRepository.findByNfcUid(uid).orElse(null);
        if (child == null) return ResponseEntity.status(404).body("Unknown card.");

        int amount = (int) body.get("amount");
        if (amount <= 0) return ResponseEntity.badRequest().body("Amount must be positive.");

        int current = child.getSeasonPoints() != null ? child.getSeasonPoints() : 0;
        if (amount > current)
            return ResponseEntity.badRequest().body("Insufficient points. Balance: " + current);

        child.setSeasonPoints(current - amount);
        childRepository.save(child);

        return ResponseEntity.ok(Map.of(
                "message", "Transaction successful.",
                "spent", amount,
                "remainingPoints", child.getSeasonPoints(),
                "name", child.getName() + " " + child.getSurname()
        ));
    }
}
