package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.DataInitializer; // Import pentru a folosi functia de generare username
import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Model.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

/**
 * Acest Controller este partea de autentificare a aplicatiei.
 * Se ocupa strict de:
 * 1. Login (Verifica credențialele).
 * 2. Register (Creeaza conturi noi).
 * * NOTA: Stergerea conturilor s-a mutat in LeaderController.
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String role = request.getRole();
        // Aici tratam 'username' ca fiind ceea ce introduce userul in campul de login.
        // Poate fi username-ul curatat SAU numele real (pentru compatibilitate in tranzitie).
        String inputUsername = request.getUsername().toLowerCase().trim();
        String rawPassword = request.getPassword(); // Parola scrisa de user (ex: "popescu123")

        // 1. Calculam varianta criptata
        String encryptedPassword = AESUtil.encrypt(rawPassword);

        // CAZUL 1: COPIL
        if ("CHILD".equalsIgnoreCase(role)) {
            List<Child> children = childRepository.findAll();
            for (Child child : children) {
                // Verificam atat dupa 'username' generat, cat si dupa 'name' (pentru conturile vechi inca nemigrate 100%)
                boolean matchFound = (child.getUsername() != null && child.getUsername().equals(inputUsername)) ||
                        (child.getName() != null && child.getName().toLowerCase().equals(inputUsername));

                if (matchFound) {
                    // VERIFICARE 1: E deja criptata in baza?
                    if (child.getPassword() != null && child.getPassword().equals(encryptedPassword)) {
                        return ResponseEntity.ok(child);
                    }
                    // VERIFICARE 2 (MIGRARE): E parola veche (necriptata)?
                    else if (child.getPassword() != null && child.getPassword().equals(rawPassword)) {
                        // O gasit-o pe cea veche! O actualizam pe loc sa fie criptata de acum incolo.
                        child.setPassword(encryptedPassword);
                        childRepository.save(child);
                        System.out.println("♻️ Parola migrata automat pentru copilul: " + child.getName());
                        return ResponseEntity.ok(child);
                    }
                }
            }
        }
        // CAZUL 2: LIDER / DIRECTOR
        else {
            List<Leader> leaders = leaderRepository.findAll();
            for (Leader leader : leaders) {
                boolean matchFound = (leader.getUsername() != null && leader.getUsername().equals(inputUsername)) ||
                        (leader.getName() != null && leader.getName().toLowerCase().equals(inputUsername));

                if (matchFound) {
                    // VERIFICARE 1: E deja criptata?
                    if (leader.getPassword() != null && leader.getPassword().equals(encryptedPassword)) {
                        if ("DIRECTOR".equalsIgnoreCase(role)) {
                            if (leader.getRole() != null && (leader.getRole().equalsIgnoreCase("Coordonator") || leader.getRole().equalsIgnoreCase("Director"))) {
                                return ResponseEntity.ok(leader);
                            }
                        } else {
                            return ResponseEntity.ok(leader);
                        }
                    }
                    // VERIFICARE 2 (MIGRARE): E parola veche?
                    else if (leader.getPassword() != null && leader.getPassword().equals(rawPassword)) {
                        // BINGO! O criptam acum.
                        leader.setPassword(encryptedPassword);
                        leaderRepository.save(leader);
                        System.out.println("♻️ Parola migrata automat pentru liderul: " + leader.getName());

                        if ("DIRECTOR".equalsIgnoreCase(role)) {
                            if (leader.getRole() != null && (leader.getRole().equalsIgnoreCase("Coordonator") || leader.getRole().equalsIgnoreCase("Director"))) {
                                return ResponseEntity.ok(leader);
                            }
                        } else {
                            return ResponseEntity.ok(leader);
                        }
                    }
                }
            }
        }
        return ResponseEntity.status(401).body("Date incorecte!");
    }

    /**
     * Metoda de INREGISTRARE (Sign Up).
     * Creeaza un cont nou (Copil sau Lider) in baza de date.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // CAZUL 1: Inregistrare COPIL
        if ("CHILD".equalsIgnoreCase(request.getRole())) {
            Child newChild = new Child();

            // Date personale
            newChild.setName(request.getName());
            newChild.setSurname(request.getSurname());

            // --- GENERARE USERNAME SIGUR ---
            String baseUsername = DataInitializer.generateCleanUsername(request.getName(), request.getSurname());
            if (childRepository.findByUsername(baseUsername).isPresent()) {
                baseUsername += new java.util.Random().nextInt(1000); // Evitam duplicatele
            }
            newChild.setUsername(baseUsername);

            // --- CRIPTARE LA INREGISTRARE ---
            newChild.setPassword(AESUtil.encrypt(request.getPassword()));

            newChild.setBirthDate(request.getBirthDate());
            newChild.setParentName(request.getParentName());
            newChild.setParentPhone(request.getParentPhone());

            // Setam valorile de start
            newChild.setSeasonPoints(0);
            newChild.setBadgesCount(0);

            // Progres
            ChildProgress initialProgress = new ChildProgress();
            initialProgress.setChild(newChild);
            initialProgress.setLastStickerId(0);
            initialProgress.setManualsCount(0);

            newChild.setProgress(initialProgress);
            newChild.setProgressPercent(0);

            // Inventar
            newChild.setHasManual(false);
            newChild.setHasShirt(false);
            newChild.setHasHat(false);

            childRepository.save(newChild);
            return ResponseEntity.ok("Cont COPIL creat! Username: " + baseUsername);
        }

        // CAZUL 2: Inregistrare LIDER
        else {
            if (!isValidCode(request.getRegistrationCode())) {
                return ResponseEntity.badRequest().body("Cod de acces invalid! Cere un cod valid de la Director.");
            }

            // Verificam duplicat
            boolean exists = leaderRepository.findAll().stream()
                    .anyMatch(l -> l.getName().equalsIgnoreCase(request.getName()) && l.getSurname().equalsIgnoreCase(request.getSurname()));

            if (exists) {
                return ResponseEntity.badRequest().body("Lider existent!");
            }

            Leader newLeader = new Leader();
            newLeader.setName(request.getName());
            newLeader.setSurname(request.getSurname());

            // --- GENERARE USERNAME SIGUR ---
            String baseUsername = DataInitializer.generateCleanUsername(request.getName(), request.getSurname());
            if (leaderRepository.findByUsername(baseUsername).isPresent()) {
                baseUsername += new java.util.Random().nextInt(1000);
            }
            newLeader.setUsername(baseUsername);

            // --- CRIPTARE LA INREGISTRARE ---
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
            return ResponseEntity.ok("Cont Lider creat cu succes! Username: " + baseUsername);
        }
    }

    /**
     * Verifica daca codul introdus la inregistrare este unul permis.
     */
    private boolean isValidCode(String code) {
        if (code == null || code.trim().isEmpty()) return false;
        List<String> validCodes = List.of("AWANA2024", "BETANIA", "DIRECTOR_KEY");
        return validCodes.contains(code.trim());
    }
}