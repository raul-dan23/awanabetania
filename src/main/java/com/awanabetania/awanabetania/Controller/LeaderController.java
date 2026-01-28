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
     * STERGERE LIDER (CORIJATĂ PENTRU HEADLEADER)
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteLeader(@PathVariable Integer id) {

        Leader leader = leaderRepository.findById(id).orElse(null);
        if (leader == null) return ResponseEntity.notFound().build();

        // 1. Îl scoatem din lista de membri (Many-to-Many)
        leader.getDepartments().clear();
        leaderRepository.save(leader);

        // 2. Îl demitem dacă este ȘEF (HeadLeader) de departament
        // CORECT: Folosim findByHeadLeaderId si setHeadLeader
        List<Department> managedDepartments = departmentRepository.findByHeadLeaderId(id);
        for (Department dept : managedDepartments) {
            dept.setHeadLeader(null); // Aici era eroarea inainte
            departmentRepository.save(dept);
        }

        // 3. Stergem notificarile personale
        notificationRepository.deleteByVisibleTo(String.valueOf(id));

        // 4. Stergem liderul
        leaderRepository.deleteById(id);

        return ResponseEntity.ok("✅ Liderul a fost șters cu succes!");
    }
}