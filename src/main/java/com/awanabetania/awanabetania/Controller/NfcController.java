package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nfc")
@CrossOrigin(origins = "*")
public class NfcController {

    @Autowired
    private ChildRepository childRepository;

    @Value("${nfc.token}")
    private String nfcToken;

    private boolean isAuthorized(String token) {
        return nfcToken != null && nfcToken.equals(token);
    }

    /**
     * Leaga un card NFC de un copil.
     * Body: { "childId": 5, "uid": "A1B2C3D4" }
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerCard(
            @RequestHeader("X-NFC-Token") String token,
            @RequestBody Map<String, Object> body) {

        if (!isAuthorized(token)) return ResponseEntity.status(403).body("Token invalid.");

        Integer childId = (Integer) body.get("childId");
        String uid = (String) body.get("uid");
        if (childId == null || uid == null || uid.isBlank())
            return ResponseEntity.badRequest().body("childId si uid sunt obligatorii.");

        // Verifica daca UID-ul e deja folosit de altcineva
        childRepository.findByNfcUid(uid).ifPresent(existing -> {
            if (!existing.getId().equals(childId)) existing.setNfcUid(null);
            childRepository.save(existing);
        });

        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();

        child.setNfcUid(uid);
        childRepository.save(child);
        return ResponseEntity.ok(Map.of(
                "message", "Card inregistrat cu succes.",
                "childId", child.getId(),
                "name", child.getName() + " " + child.getSurname(),
                "uid", uid
        ));
    }

    /**
     * Returneaza datele copilului dupa UID.
     * Folosit de JAR-ul de la taraba dupa scanare.
     */
    @GetMapping("/{uid}")
    public ResponseEntity<?> getChildByUid(
            @PathVariable String uid,
            @RequestHeader("X-NFC-Token") String token) {

        if (!isAuthorized(token)) return ResponseEntity.status(403).body("Token invalid.");

        Child child = childRepository.findByNfcUid(uid).orElse(null);
        if (child == null) return ResponseEntity.status(404).body("Card necunoscut.");

        return ResponseEntity.ok(Map.of(
                "id", child.getId(),
                "name", child.getName(),
                "surname", child.getSurname(),
                "seasonPoints", child.getSeasonPoints() != null ? child.getSeasonPoints() : 0,
                "uid", uid
        ));
    }

    /**
     * Scade puncte din seasonPoints.
     * Body: { "amount": 50 }
     */
    @PostMapping("/{uid}/spend")
    public ResponseEntity<?> spendPoints(
            @PathVariable String uid,
            @RequestHeader("X-NFC-Token") String token,
            @RequestBody Map<String, Object> body) {

        if (!isAuthorized(token)) return ResponseEntity.status(403).body("Token invalid.");

        Child child = childRepository.findByNfcUid(uid).orElse(null);
        if (child == null) return ResponseEntity.status(404).body("Card necunoscut.");

        int amount = (int) body.get("amount");
        if (amount <= 0) return ResponseEntity.badRequest().body("Suma trebuie sa fie pozitiva.");

        int current = child.getSeasonPoints() != null ? child.getSeasonPoints() : 0;
        if (amount > current)
            return ResponseEntity.badRequest().body("Puncte insuficiente. Sold: " + current);

        child.setSeasonPoints(current - amount);
        childRepository.save(child);

        return ResponseEntity.ok(Map.of(
                "message", "Tranzactie reusita.",
                "spent", amount,
                "remainingPoints", child.getSeasonPoints(),
                "name", child.getName() + " " + child.getSurname()
        ));
    }
}
