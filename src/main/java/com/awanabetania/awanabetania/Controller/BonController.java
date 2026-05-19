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

@RestController
@RequestMapping("/api/bons")
@CrossOrigin(origins = "*")
public class BonController {

    @Autowired private BonRepository bonRepository;
    @Autowired private ChildRepository childRepository;

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

    @PostMapping
    public ResponseEntity<?> createBon(@RequestBody Map<String, Object> body) {
        Integer childId = (Integer) body.get("childId");
        String leaderName = (String) body.get("leaderName");
        String items = (String) body.get("items");
        Integer totalPoints = (Integer) body.get("totalPoints");

        if (childId == null || items == null || totalPoints == null)
            return ResponseEntity.badRequest().body("Date incomplete.");

        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) return ResponseEntity.notFound().build();

        Bon bon = new Bon();
        bon.setChild(child);
        bon.setLeaderName(leaderName);
        bon.setItems(items);
        bon.setTotalPoints(totalPoints);

        return ResponseEntity.ok(toBonMap(bonRepository.save(bon)));
    }

    @GetMapping("/pending")
    public List<Map<String, Object>> getPending() {
        return bonRepository.findByStatusOrderByCreatedAtDesc("PENDING")
                .stream().map(this::toBonMap).collect(Collectors.toList());
    }

    @GetMapping("/all")
    public List<Map<String, Object>> getAll() {
        return bonRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toBonMap).collect(Collectors.toList());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Integer id) {
        Bon bon = bonRepository.findById(id).orElse(null);
        if (bon == null) return ResponseEntity.notFound().build();
        if (!"PENDING".equals(bon.getStatus()))
            return ResponseEntity.badRequest().body("Bonul nu este în așteptare.");

        Child child = bon.getChild();
        int current = child.getSeasonPoints() != null ? child.getSeasonPoints() : 0;
        if (bon.getTotalPoints() > current)
            return ResponseEntity.badRequest().body(
                "Puncte insuficiente. Sold: " + current + ", necesar: " + bon.getTotalPoints()
            );

        child.setSeasonPoints(current - bon.getTotalPoints());
        childRepository.save(child);

        bon.setStatus("APPROVED");
        bon.setApprovedAt(LocalDateTime.now());
        bonRepository.save(bon);

        return ResponseEntity.ok(Map.of(
            "message", "Bon aprobat.",
            "remainingPoints", child.getSeasonPoints(),
            "childName", child.getName() + " " + child.getSurname()
        ));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Integer id) {
        Bon bon = bonRepository.findById(id).orElse(null);
        if (bon == null) return ResponseEntity.notFound().build();
        if (!"PENDING".equals(bon.getStatus()))
            return ResponseEntity.badRequest().body("Bonul nu este în așteptare.");

        bon.setStatus("REJECTED");
        bonRepository.save(bon);

        return ResponseEntity.ok(Map.of("message", "Bon respins."));
    }
}
