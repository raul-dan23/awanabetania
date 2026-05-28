package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.DataInitializer;
import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Handles authentication (login) and account registration for both children and leaders.
 * Passwords are AES-128 encrypted before storage. Login accepts either a generated username
 * or the legacy plain name to support accounts created before the username migration.
 * Plain-text passwords found during login are transparently upgraded to encrypted form.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private LeaderRepository leaderRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * Authenticates a user and returns their full entity on success.
     * The lookup order is: username (indexed) → name fallback (for legacy accounts).
     * Passwords are compared against the encrypted form; a plain-text match triggers
     * an in-place upgrade to encrypted storage.
     * For the DIRECTOR role, only leaders with role "Director" or "Coordonator" are accepted.
     *
     * @param request login payload containing username, password, and role
     * @return 200 with the Child or Leader entity on success; 401 on invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String role = request.getRole();
        String inputUsername = request.getUsername().toLowerCase().trim();
        String rawPassword = request.getPassword();
        String encryptedPassword = AESUtil.encrypt(rawPassword);

        if ("CHILD".equalsIgnoreCase(role)) {
            Optional<Child> childOpt = childRepository.findByUsername(inputUsername);
            if (childOpt.isEmpty()) {
                childOpt = childRepository.findByNameIgnoreCase(inputUsername);
            }
            if (childOpt.isPresent()) {
                Child child = childOpt.get();
                if (child.getPassword() != null && child.getPassword().equals(encryptedPassword)) {
                    return ResponseEntity.ok(child);
                } else if (child.getPassword() != null && child.getPassword().equals(rawPassword)) {
                    // Upgrade plain-text password to encrypted
                    child.setPassword(encryptedPassword);
                    childRepository.save(child);
                    return ResponseEntity.ok(child);
                }
            }
        } else {
            Optional<Leader> leaderOpt = leaderRepository.findByUsername(inputUsername);
            if (leaderOpt.isEmpty()) {
                leaderOpt = leaderRepository.findByNameIgnoreCase(inputUsername);
            }
            if (leaderOpt.isPresent()) {
                Leader leader = leaderOpt.get();
                boolean passwordMatch = false;
                if (leader.getPassword() != null && leader.getPassword().equals(encryptedPassword)) {
                    passwordMatch = true;
                } else if (leader.getPassword() != null && leader.getPassword().equals(rawPassword)) {
                    // Upgrade plain-text password to encrypted
                    leader.setPassword(encryptedPassword);
                    leaderRepository.save(leader);
                    passwordMatch = true;
                }
                if (passwordMatch) {
                    if ("DIRECTOR".equalsIgnoreCase(role)) {
                        // Only directors and coordinators may log in with the director role
                        if (leader.getRole() != null &&
                                (leader.getRole().equalsIgnoreCase("Coordonator") ||
                                 leader.getRole().equalsIgnoreCase("Director"))) {
                            return ResponseEntity.ok(leader);
                        }
                    } else {
                        return ResponseEntity.ok(leader);
                    }
                }
            }
        }

        return ResponseEntity.status(401).body("Invalid credentials.");
    }

    /**
     * Creates a new Child or Leader account.
     * A unique username is generated from the name and surname. If the base username is
     * already taken, a random 3-digit suffix is appended.
     * Leaders must supply a valid registration code; the password is AES-encrypted before storage.
     *
     * @param request registration payload (role, name, surname, password, plus role-specific fields)
     * @return 200 with a confirmation message and generated username on success;
     *         400 if validation fails (duplicate leader, invalid code)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if ("CHILD".equalsIgnoreCase(request.getRole())) {
            Child newChild = new Child();
            newChild.setName(request.getName());
            newChild.setSurname(request.getSurname());

            String baseUsername = DataInitializer.generateCleanUsername(request.getName(), request.getSurname());
            if (childRepository.findByUsername(baseUsername).isPresent()) {
                baseUsername += new java.util.Random().nextInt(1000);
            }
            newChild.setUsername(baseUsername);
            newChild.setPassword(AESUtil.encrypt(request.getPassword()));
            newChild.setBirthDate(request.getBirthDate());
            newChild.setParentName(request.getParentName());
            newChild.setParentPhone(request.getParentPhone());
            newChild.setSeasonPoints(0);
            newChild.setBadgesCount(0);

            ChildProgress initialProgress = new ChildProgress();
            initialProgress.setChild(newChild);
            initialProgress.setLastStickerId(0);
            initialProgress.setManualsCount(0);
            newChild.setProgress(initialProgress);
            newChild.setProgressPercent(0);
            newChild.setHasManual(false);
            newChild.setHasShirt(false);
            newChild.setHasHat(false);

            childRepository.save(newChild);
            return ResponseEntity.ok("Child account created! Username: " + baseUsername);
        } else {
            if (!isValidCode(request.getRegistrationCode())) {
                return ResponseEntity.badRequest().body("Invalid registration code. Ask the director for a valid code.");
            }

            boolean exists = leaderRepository.findByNameAndSurname(request.getName(), request.getSurname()).isPresent();
            if (exists) {
                return ResponseEntity.badRequest().body("A leader with this name already exists.");
            }

            Leader newLeader = new Leader();
            newLeader.setName(request.getName());
            newLeader.setSurname(request.getSurname());

            String baseUsername = DataInitializer.generateCleanUsername(request.getName(), request.getSurname());
            if (leaderRepository.findByUsername(baseUsername).isPresent()) {
                baseUsername += new java.util.Random().nextInt(1000);
            }
            newLeader.setUsername(baseUsername);
            newLeader.setPassword(AESUtil.encrypt(request.getPassword()));
            newLeader.setRole(request.getRole());
            newLeader.setPhoneNumber(request.getPhoneNumber());
            newLeader.setRating(0.0f);

            if (request.getDepartmentIds() != null && !request.getDepartmentIds().isEmpty()) {
                Set<Department> selectedDepts = new HashSet<>();
                for (Integer deptId : request.getDepartmentIds()) {
                    departmentRepository.findById(deptId).ifPresent(selectedDepts::add);
                }
                newLeader.setDepartments(selectedDepts);
            }

            leaderRepository.save(newLeader);
            return ResponseEntity.ok("Leader account created! Username: " + baseUsername);
        }
    }

    /**
     * Validates a leader registration code against the hard-coded list of accepted codes.
     *
     * @param code the code submitted by the registrant
     * @return {@code true} if the code is in the accepted list
     */
    private boolean isValidCode(String code) {
        if (code == null || code.trim().isEmpty()) return false;
        List<String> validCodes = List.of("AWANA2024", "BETANIA", "DIRECTOR_KEY");
        return validCodes.contains(code.trim());
    }
}
