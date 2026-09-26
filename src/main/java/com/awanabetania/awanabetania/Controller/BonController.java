package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Bon;
import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.BonRepository;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages purchase receipts (bons) for the end-of-season fair.
 * A selling leader creates a receipt; the cashier then approves or rejects it
 * after scanning the child's NFC card. Approval atomically deducts points from
 * the child's {@code seasonPoints} balance after checking that sufficient funds exist.
 */
@RestController
@RequestMapping("/api/bons")
public class BonController {

    @Autowired private BonRepository bonRepository;
    @Autowired private ChildRepository childRepository;

    /**
     * Converts a {@link Bon} entity into a flat map safe for JSON serialization.
     * Includes live child data (current point balance) alongside the receipt fields.
     */
    private Map<String, Object> toBonMap(Bon bon) {
        Child child = bon.getChild();
        Map<String, Object> map = new HashMap<>();
        map.put("id", bon.getId());
        map.put("childId", child.getId());
        map.put("childName", child.getName() + " " + child.getSurname());
        map.put("childPoints", child.getSeasonPoints() != null ? child.getSeasonPoints() : 0);
        map.put("leaderName", bon.getLeaderName() != null ? bon.getLeaderName() : "");
        map.put("items", bon.getItems());
        map.put("totalPoints", bon.getTotalPoints());
        map.put("status", bon.getStatus());
        map.put("createdAt", bon.getCreatedAt() != null ? bon.getCreatedAt().toString() : "");
        return map;
    }

    /**
     * Creates a new PENDING receipt.
     *
     * @param body JSON with keys "childId", "leaderName", "items" (JSON string), "totalPoints"
     * @return 200 with the saved receipt as a flat map; 400 if required fields are missing; 404 if child not found
     */
    @PostMapping
    public ResponseEntity<?> createBon(@RequestBody Map<String, Object> body) {
        Integer childId = (Integer) body.get("childId");
        String leaderName = (String) body.get("leaderName");
        String items = (String) body.get("items");
        Integer totalPoints = (Integer) body.get("totalPoints");

        if (childId == null || items == null || totalPoints == null)
            return ResponseEntity.badRequest().body("Incomplete data.");

        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();

        Bon bon = new Bon();
        bon.setChild(child);
        bon.setLeaderName(leaderName);
        bon.setItems(items);
        bon.setTotalPoints(totalPoints);

        return ResponseEntity.ok(toBonMap(bonRepository.save(bon)));
    }

    /**
     * Returns all PENDING receipts, newest first.
     *
     * @return list of pending receipts as flat maps
     */
    @GetMapping("/pending")
    public List<Map<String, Object>> getPending() {
        return bonRepository.findByStatusOrderByCreatedAtDesc("PENDING")
                .stream().map(this::toBonMap).collect(Collectors.toList());
    }

    /**
     * Returns all receipts regardless of status, newest first.
     *
     * @return list of all receipts as flat maps
     */
    @GetMapping("/all")
    public List<Map<String, Object>> getAll() {
        return bonRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toBonMap).collect(Collectors.toList());
    }

    /**
     * Approves a PENDING receipt and deducts the point cost from the child's balance.
     * Fails if the receipt is not PENDING or the child has insufficient points.
     *
     * @param id the receipt's primary key
     * @return 200 with remaining points and child name on success;
     *         400 if not pending or insufficient balance; 404 if not found
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Integer id) {
        Bon bon = bonRepository.findById(id).orElse(null);
        if (bon == null) return ResponseEntity.notFound().build();
        if (!"PENDING".equals(bon.getStatus()))
            return ResponseEntity.badRequest().body("Receipt is not pending.");

        Child child = bon.getChild();
        int current = child.getSeasonPoints() != null ? child.getSeasonPoints() : 0;
        if (bon.getTotalPoints() > current)
            return ResponseEntity.badRequest().body(
                    "Insufficient points. Balance: " + current + ", required: " + bon.getTotalPoints());

        child.setSeasonPoints(current - bon.getTotalPoints());
        childRepository.save(child);

        bon.setStatus("APPROVED");
        bon.setApprovedAt(LocalDateTime.now());
        bonRepository.save(bon);

        return ResponseEntity.ok(Map.of(
                "message", "Receipt approved.",
                "remainingPoints", child.getSeasonPoints(),
                "childName", child.getName() + " " + child.getSurname()
        ));
    }

    /**
     * Rejects a PENDING receipt without modifying the child's point balance.
     *
     * @param id the receipt's primary key
     * @return 200 on success; 400 if not pending; 404 if not found
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Integer id) {
        Bon bon = bonRepository.findById(id).orElse(null);
        if (bon == null) return ResponseEntity.notFound().build();
        if (!"PENDING".equals(bon.getStatus()))
            return ResponseEntity.badRequest().body("Receipt is not pending.");

        bon.setStatus("REJECTED");
        bonRepository.save(bon);

        return ResponseEntity.ok(Map.of("message", "Receipt rejected."));
    }
}
