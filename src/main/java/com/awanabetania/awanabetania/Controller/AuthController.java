package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.DataInitializer;
import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Security.JwtService;
import com.awanabetania.awanabetania.Service.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Handles authentication (login) and account registration for both children and leaders.
 * Passwords are hashed with BCrypt; legacy AES and plain-text values are rehashed the first
 * time their owner logs in. Login accepts either a generated username or the legacy plain
 * name to support accounts created before the username migration, and returns a signed JWT
 * that the client must present on every subsequent request.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordService passwordService;

    /** Leader registration codes, supplied via the AUTH_REGISTRATION_CODES environment variable. */
    @Value("${auth.registration-codes}")
    private String registrationCodes;

    @Autowired
    private LeaderRepository leaderRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * Authenticates a user and returns a signed token together with their account.
     * The lookup order is: username (indexed) → name fallback (for legacy accounts).
     * For the DIRECTOR role, only leaders with role "Director" or "Coordonator" are accepted.
     *
     * @param request login payload containing username, password, and role
     * @return 200 with {@code {token, user}} on success; 401 on invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body("Username and password are required.");
        }
        String role = request.getRole();
        String inputUsername = request.getUsername().toLowerCase().trim();
        String rawPassword = request.getPassword();

        if ("CHILD".equalsIgnoreCase(role)) {
            Optional<Child> childOpt = childRepository.findByUsername(inputUsername);
            if (childOpt.isEmpty()) {
                childOpt = childRepository.findByNameIgnoreCase(inputUsername);
            }
            if (childOpt.isPresent()) {
                Child child = childOpt.get();
                if (matchesAndUpgrade(child.getPassword(), rawPassword, child::setPassword,
                                      () -> childRepository.save(child))) {
                    return ResponseEntity.ok(session(
                            jwtService.issue(child.getUsername(), "CHILD", child.getId(), null),
                            child, child.isPasswordChangeRequired()));
                }
            }
        } else {
            Optional<Leader> leaderOpt = leaderRepository.findByUsername(inputUsername);
            if (leaderOpt.isEmpty()) {
                leaderOpt = leaderRepository.findByNameIgnoreCase(inputUsername);
            }
            if (leaderOpt.isPresent()) {
                Leader leader = leaderOpt.get();
                if (matchesAndUpgrade(leader.getPassword(), rawPassword, leader::setPassword,
                                      () -> leaderRepository.save(leader))) {
                    if ("DIRECTOR".equalsIgnoreCase(role)) {
                        // Only directors and coordinators may log in with the director role
                        if (leader.getRole() == null ||
                                !(leader.getRole().equalsIgnoreCase("Coordonator") ||
                                  leader.getRole().equalsIgnoreCase("Director"))) {
                            return ResponseEntity.status(401).body("Invalid credentials.");
                        }
                    }
                    return ResponseEntity.ok(session(
                            jwtService.issue(leader.getUsername(), "LEADER", leader.getId(), leader.getRole()),
                            leader, leader.isPasswordChangeRequired()));
                }
            }
        }

        return ResponseEntity.status(401).body("Invalid credentials.");
    }

    /**
     * Verifies a submitted password against the stored value and migrates legacy
     * credentials to BCrypt on the fly.
     * <p>
     * Accounts created before this change hold either a plain-text password or an
     * AES ciphertext. Both are accepted once, then immediately rehashed with BCrypt and
     * saved, so existing users keep logging in with the password they already know and
     * the reversible forms disappear from the database as people sign in.
     *
     * @param stored  the password value currently held for the account
     * @param raw     the plain-text password submitted by the caller
     * @param setter  applies the freshly computed BCrypt hash to the entity
     * @param save    persists the entity after an upgrade
     * @return {@code true} when the submitted password is correct
     */
    private boolean matchesAndUpgrade(String stored, String raw,
                                      java.util.function.Consumer<String> setter,
                                      Runnable save) {
        if (stored == null || raw == null) return false;

        // Already migrated: a BCrypt hash always starts with $2a/$2b/$2y.
        if (stored.startsWith("$2")) {
            return passwordEncoder.matches(raw, stored);
        }

        // Legacy: AES ciphertext, or a plain-text password from the earliest accounts.
        boolean legacyMatch = stored.equals(AESUtil.encrypt(raw)) || stored.equals(raw);
        if (legacyMatch) {
            setter.accept(passwordEncoder.encode(raw));
            save.run();
            return true;
        }
        return false;
    }

    /**
     * Builds the login response: the signed token, the account entity, and whether the
     * account must choose a new password first (after a reset by the director).
     * The entity's password field is annotated write-only, so no credential material
     * is serialised here.
     */
    private Map<String, Object> session(String token, Object user, boolean mustChangePassword) {
        Map<String, Object> body = new HashMap<>();
        body.put("token", token);
        body.put("user", user);
        body.put("mustChangePassword", mustChangePassword);
        return body;
    }

    /**
     * Creates a new Child or Leader account.
     * A unique username is generated from the name and surname. If the base username is
     * already taken, a random 3-digit suffix is appended.
     * Leaders must supply a valid registration code; the password is BCrypt-hashed before storage.
     *
     * @param request registration payload (role, name, surname, password, plus role-specific fields)
     * @return 200 with a confirmation message and generated username on success;
     *         400 if validation fails (duplicate leader, invalid code)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (isBlank(request.getName()) || isBlank(request.getSurname()) || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest().body("Name, surname and password are required.");
        }

        if ("CHILD".equalsIgnoreCase(request.getRole())) {
            Child newChild = new Child();
            newChild.setName(request.getName());
            newChild.setSurname(request.getSurname());

            String baseUsername = DataInitializer.generateCleanUsername(request.getName(), request.getSurname());
            if (childRepository.findByUsername(baseUsername).isPresent()) {
                baseUsername += new java.util.Random().nextInt(1000);
            }
            newChild.setUsername(baseUsername);
            newChild.setPassword(passwordService.hash(request.getPassword()));
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
            newLeader.setPassword(passwordService.hash(request.getPassword()));
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Validates a leader registration code against the configured list.
     * <p>
     * The codes previously lived in this file, which meant anyone reading the public
     * repository could register themselves as a director. They now come from the
     * {@code AUTH_REGISTRATION_CODES} environment variable and can be rotated without
     * a code change.
     *
     * @param code the code submitted by the registrant
     * @return {@code true} if the code is in the configured list
     */
    private boolean isValidCode(String code) {
        if (code == null || code.trim().isEmpty()) return false;
        return Arrays.stream(registrationCodes.split(","))
                .map(String::trim)
                .filter(c -> !c.isEmpty())
                .anyMatch(c -> c.equals(code.trim()));
    }
}
