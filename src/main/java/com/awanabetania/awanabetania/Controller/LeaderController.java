package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Department;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Repository.*;
import com.awanabetania.awanabetania.Model.AESUtil; // <--- IMPORT NOU
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaders")
@CrossOrigin(origins = "*")
public class LeaderController {

    @Autowired
    private LeaderRepository leaderRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // --- REPOSITORY-URI PENTRU CURATENIE (Dependency Cleanup) ---
    @Autowired
    private MeetingAssignmentRepository meetingAssignmentRepository;

    @Autowired
    private LeaderEvaluationRepository leaderEvaluationRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    // --- METODE STANDARD (GET, PUT) ---

    /**
     * Returneaza lista tuturor liderilor.
     */
    @GetMapping
    public List<Leader> getAllLeaders() {
        return leaderRepository.findAll();
    }

    /**
     * Cauta un lider dupa ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Leader> getLeaderById(@PathVariable Integer id) {
        return leaderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Actualizeaza datele unui lider.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Leader> updateLeader(@PathVariable Integer id, @RequestBody Leader leaderDetails) {
        return leaderRepository.findById(id).map(leader -> {
            leader.setName(leaderDetails.getName());
            leader.setSurname(leaderDetails.getSurname());
            leader.setPhoneNumber(leaderDetails.getPhoneNumber());
            leader.setNotes(leaderDetails.getNotes());

            // --- CRIPTARE LA ACTUALIZARE ---
            if (leaderDetails.getPassword() != null && !leaderDetails.getPassword().trim().isEmpty()) {
                // Daca parola vine din frontend in clar (ex: "1234"), o criptam
                String encryptedPass = AESUtil.encrypt(leaderDetails.getPassword());
                leader.setPassword(encryptedPass);
            }

            // Actualizare departamente
            if (leaderDetails.getDepartments() != null) {
                leader.getDepartments().clear();
                for (Department d : leaderDetails.getDepartments()) {
                    departmentRepository.findById(d.getId()).ifPresent(leader.getDepartments()::add);
                }
            }
            return ResponseEntity.ok(leaderRepository.save(leader));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- METODA DE STERGERE (RAMANE IDENTICA, DOAR COPIATA AICI PENTRU COMPLETITUDINE) ---

    /**
     * Sterge un lider si toate datele asociate acestuia.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteLeader(@PathVariable Integer id, @RequestParam(required = false) String code) {

        Leader leader = leaderRepository.findById(id).orElse(null);
        if (leader == null) return ResponseEntity.notFound().build();

        // --- 1. DEBUGGING ---
        System.out.println("====== TENTATIVA STERGERE LIDER ID: " + id + " ======");

        // Curatam spatiile goale
        String inputCode = (code != null) ? code.trim() : "";
        String dbCode = (leader.getDeletionCode() != null) ? leader.getDeletionCode().trim() : "";

        List<String> masterCodes = List.of("AWANA2024", "BETANIA", "DIRECTOR_KEY", "ADMIN");
        boolean isMasterCode = masterCodes.stream().anyMatch(mc -> mc.equalsIgnoreCase(inputCode));

        boolean isUserCodeCorrect = false;

        if (!dbCode.isEmpty()) {
            if (inputCode.equalsIgnoreCase(dbCode)) {
                isUserCodeCorrect = true;
            }
        } else {
            isUserCodeCorrect = true;
        }

        if (!isMasterCode && !isUserCodeCorrect) {
            return ResponseEntity.badRequest().body("❌ Cod incorect!");
        }

        // --- 3. CURATENIA ---
        leader.getDepartments().clear();
        leaderRepository.save(leader);

        List<Department> managedDepartments = departmentRepository.findByHeadLeaderId(id);
        for (Department dept : managedDepartments) {
            dept.setHeadLeader(null);
            departmentRepository.save(dept);
        }

        meetingAssignmentRepository.deleteByLeaderId(id);
        leaderEvaluationRepository.deleteByLeaderId(id);
        leaderEvaluationRepository.deleteByEvaluatedBy(id);

        List<Meeting> meetings = meetingRepository.findByDirectorDayId(id);
        for (Meeting m : meetings) {
            m.setDirectorDay(null);
            meetingRepository.save(m);
        }

        notificationRepository.deleteByVisibleTo(String.valueOf(id));
        leaderRepository.deleteById(id);

        return ResponseEntity.ok("✅ Liderul a fost sters cu succes!");
    }
}