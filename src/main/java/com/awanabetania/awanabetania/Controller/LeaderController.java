package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Department;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.NotificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaders")
@CrossOrigin(origins = "*")
public class LeaderController {

    @Autowired private LeaderRepository leaderRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private NotificationRepository notificationRepository;

    @GetMapping
    public List<Leader> getAllLeaders() { return leaderRepository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Leader> getLeaderById(@PathVariable Integer id) {
        return leaderRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Leader> updateLeader(@PathVariable Integer id, @RequestBody Leader leaderDetails) {
        return leaderRepository.findById(id).map(leader -> {
            leader.setName(leaderDetails.getName());
            leader.setSurname(leaderDetails.getSurname());
            leader.setPhoneNumber(leaderDetails.getPhoneNumber());
            leader.setNotes(leaderDetails.getNotes());
            if (leaderDetails.getPassword() != null && !leaderDetails.getPassword().trim().isEmpty()) {
                leader.setPassword(leaderDetails.getPassword());
            }
            if (leaderDetails.getDepartments() != null) {
                leader.getDepartments().clear();
                for (Department d : leaderDetails.getDepartments()) {
                    departmentRepository.findById(d.getId()).ifPresent(leader.getDepartments()::add);
                }
            }
            return ResponseEntity.ok(leaderRepository.save(leader));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * STERGERE LIDER CU COD DE SECURITATE
     * Endpoint: DELETE /api/leaders/{id}?code=A1B2
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteLeader(@PathVariable Integer id, @RequestParam(required = false) String code) { // <--- PARAMETRU NOU

        Leader leader = leaderRepository.findById(id).orElse(null);
        if (leader == null) return ResponseEntity.notFound().build();

        // --- VERIFICARE COD (Daca e cerut) ---
        // Poti scoate 'required=false' daca vrei sa fie obligatoriu mereu.
        // Aici verificam: daca are un cod setat in baza, trebuie sa coincida cu ce primim.
        if (leader.getDeletionCode() != null && !leader.getDeletionCode().isEmpty()) {
            if (code == null || !code.equalsIgnoreCase(leader.getDeletionCode())) {
                return ResponseEntity.badRequest().body("❌ Codul de ștergere este incorect!");
            }
        }

        // --- LOGICA DE CURATENIE (Aceeasi ca inainte) ---
        leader.getDepartments().clear();
        leaderRepository.save(leader);

        List<Department> managedDepartments = departmentRepository.findByHeadLeaderId(id);
        for (Department dept : managedDepartments) {
            dept.setHeadLeader(null);
            departmentRepository.save(dept);
        }

        notificationRepository.deleteByVisibleTo(String.valueOf(id));
        leaderRepository.deleteById(id);

        return ResponseEntity.ok("✅ Liderul a fost șters cu succes!");
    }
}