package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.*;
import com.awanabetania.awanabetania.Security.AuthUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages leader accounts: listing, profile updates, and account deletion.
 * Only the account owner or a director/coordinator may edit or delete a leader, and
 * deletion additionally requires the account's pre-generated deletion code.
 * All relational data (department memberships, meeting assignments, evaluations,
 * director-of-day references) is cleaned up before the leader record is removed.
 */
@RestController
@RequestMapping("/api/leaders")
public class LeaderController {

    @Autowired private LeaderRepository leaderRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private MeetingAssignmentRepository meetingAssignmentRepository;
    @Autowired private LeaderEvaluationRepository leaderEvaluationRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /** The account owner, or a director/coordinator acting on someone else's account. */
    private static boolean mayManage(Integer leaderId) {
        AuthUser me = AuthUser.current();
        return me != null && (me.is(AuthUser.LEADER, leaderId) || me.isDirector());
    }

    /**
     * Returns all leaders registered in the club.
     *
     * @return list of all {@link Leader} entities
     */
    @GetMapping
    public List<Leader> getAllLeaders() {
        return leaderRepository.findAll();
    }

    /**
     * Returns a single leader by ID.
     *
     * @param id the leader's primary key
     * @return 200 with the leader entity, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Leader> getLeaderById(@PathVariable Integer id) {
        return leaderRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing leader's profile.
     * A non-empty password in the request body replaces the stored one and is BCrypt-hashed.
     * Only the account owner or a director/coordinator may do this.
     * Department membership is updated if provided.
     *
     * @param id            the leader's primary key
     * @param leaderDetails updated fields from the request body
     * @return 200 with the updated entity, 400 on duplicate username, 403 if the caller may not
     *         edit this account, or 404 if not found
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateLeader(@PathVariable Integer id, @RequestBody Leader leaderDetails) {
        if (!mayManage(id)) return ResponseEntity.status(403).body("You can only edit your own profile.");
        return leaderRepository.findById(id).map(leader -> {
            var existingUser = leaderRepository.findByUsername(leaderDetails.getUsername());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body("This username is already taken.");
            }

            leader.setName(leaderDetails.getName());
            leader.setSurname(leaderDetails.getSurname());
            leader.setUsername(leaderDetails.getUsername());
            leader.setPhoneNumber(leaderDetails.getPhoneNumber());

            if (leaderDetails.getDepartments() != null) {
                leader.setDepartments(leaderDetails.getDepartments());
            }

            if (leaderDetails.getPassword() != null && !leaderDetails.getPassword().isEmpty()) {
                leader.setPassword(passwordEncoder.encode(leaderDetails.getPassword()));
            }

            return ResponseEntity.ok(leaderRepository.save(leader));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Permanently deletes a leader account and all associated records.
     * Requires the account-specific deletion code; the caller must be the account owner or a
     * director/coordinator. (The former master codes were public on GitHub and let any
     * logged-in user delete any leader.)
     * Cleans up department memberships, head-of-department references, meeting assignments,
     * evaluations (given and received), and director-of-day references before deletion.
     *
     * @param id   the leader's primary key
     * @param code the deletion-confirmation code
     * @return 200 on success, 400 on incorrect code, 403 if the caller may not delete this
     *         account, or 404 if not found
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteLeader(@PathVariable Integer id, @RequestParam(required = false) String code) {
        if (!mayManage(id)) return ResponseEntity.status(403).body("You can only delete your own account.");
        Leader leader = leaderRepository.findById(id).orElse(null);
        if (leader == null) return ResponseEntity.notFound().build();

        String inputCode = (code != null) ? code.trim() : "";
        String dbCode = (leader.getDeletionCode() != null) ? leader.getDeletionCode().trim() : "";

        if (dbCode.isEmpty() || !inputCode.equalsIgnoreCase(dbCode)) {
            return ResponseEntity.badRequest().body("Incorrect deletion code.");
        }

        // Clear bidirectional relationships before deletion to avoid FK constraint errors
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
        return ResponseEntity.ok("Leader account deleted successfully.");
    }
}
