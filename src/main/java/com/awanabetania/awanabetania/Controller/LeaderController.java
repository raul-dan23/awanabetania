package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Department;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Repository.*;
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
    @Autowired private MeetingAssignmentRepository meetingAssignmentRepository;
    @Autowired private LeaderEvaluationRepository leaderEvaluationRepository;
    @Autowired private MeetingRepository meetingRepository;

    @GetMapping
    public List<Leader> getAllLeaders() { return leaderRepository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Leader> getLeaderById(@PathVariable Integer id) {
        return leaderRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateLeader(@PathVariable Integer id, @RequestBody Leader leaderDetails) {
        return leaderRepository.findById(id).map(leader -> {
            // Validare username unic
            var existingUser = leaderRepository.findByUsername(leaderDetails.getUsername());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body("❌ Acest username este deja folosit!");
            }

            leader.setName(leaderDetails.getName());
            leader.setSurname(leaderDetails.getSurname());
            leader.setUsername(leaderDetails.getUsername()); // SALVARE USERNAME
            leader.setPhoneNumber(leaderDetails.getPhoneNumber());

            // Actualizăm departamentele dacă sunt trimise din Profil
            if (leaderDetails.getDepartments() != null) {
                leader.setDepartments(leaderDetails.getDepartments());
            }

            if (leaderDetails.getPassword() != null && !leaderDetails.getPassword().isEmpty()) {
                leader.setPassword(leaderDetails.getPassword());
            }

            return ResponseEntity.ok(leaderRepository.save(leader));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteLeader(@PathVariable Integer id, @RequestParam(required = false) String code) {
        Leader leader = leaderRepository.findById(id).orElse(null);
        if (leader == null) return ResponseEntity.notFound().build();

        // Validare Cod
        String inputCode = (code != null) ? code.trim() : "";
        String dbCode = (leader.getDeletionCode() != null) ? leader.getDeletionCode().trim() : "";

        List<String> masterCodes = List.of("AWANA2024", "BETANIA", "ADMIN");
        boolean isMaster = masterCodes.stream().anyMatch(mc -> mc.equalsIgnoreCase(inputCode));

        if (!isMaster && !inputCode.equalsIgnoreCase(dbCode)) {
            return ResponseEntity.badRequest().body("❌ Cod de ștergere incorect!");
        }

        // --- CURĂȚENIE RELAȚII (Prevenire Eroare 500) ---
        leader.getDepartments().clear();
        leaderRepository.save(leader);

        departmentRepository.findByHeadLeaderId(id).forEach(dept -> {
            dept.setHeadLeader(null);
            departmentRepository.save(dept);
        });

        meetingAssignmentRepository.deleteByLeaderId(id);
        leaderEvaluationRepository.deleteByLeaderId(id);
        leaderEvaluationRepository.deleteByEvaluatedBy(id);

        meetingRepository.findByDirectorDayId(id).forEach(m -> {
            m.setDirectorDay(null);
            meetingRepository.save(m);
        });

        leaderRepository.delete(leader);
        return ResponseEntity.ok("✅ Lider sters cu succes!");
    }
}