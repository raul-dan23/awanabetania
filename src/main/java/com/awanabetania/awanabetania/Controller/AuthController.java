package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Department;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.LoginRequest;
import com.awanabetania.awanabetania.Model.RegisterRequest;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Acest Controller este partea de autentificare a aplicatiei.
 * Se ocupa de doua lucruri critice:
 * 1. Login (Verifica daca ai voie sa intri).
 * 2. Register (Creeaza conturi noi, dar cu reguli stricte pentru lideri).
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    // Avem nevoie de acces la tabelele din baza de date
    @Autowired
    private LeaderRepository leaderRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * Metoda de LOGIN.
     * Primeste un pachet JSON cu username, password si role.
     * Returneaza datele utilizatorului daca totul e corect, sau eroare 401.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String role = request.getRole();
        String username = request.getUsername();
        String password = request.getPassword();

        // CAZUL 1: Daca cel care vrea sa intre este COPIL
        if ("CHILD".equalsIgnoreCase(role)) {
            // Luam toti copiii din baza de date
            List<Child> children = childRepository.findAll();

            // Cautam manual copilul care are acelasi nume si aceeasi parola
            for (Child child : children) {
                if (child.getName().equalsIgnoreCase(username) &&
                        (child.getPassword() != null && child.getPassword().equals(password))) {
                    return ResponseEntity.ok(child); // Am gasit copilul, il lasam sa intre
                }
            }
        }
        // CAZUL 2: Daca este LIDER sau DIRECTOR
        else {
            List<Leader> leaders = leaderRepository.findAll();
            for (Leader leader : leaders) {
                // Verificam daca numele si parola corespund
                if (leader.getName().equalsIgnoreCase(username) && leader.getPassword().equals(password)) {

                    // Verificare suplimentara: Daca vrea sa intre ca DIRECTOR,
                    // ne asiguram ca are si gradul necesar in baza de date (Coordonator sau Director).
                    if ("DIRECTOR".equalsIgnoreCase(role)) {
                        if (leader.getRole() != null && (leader.getRole().equalsIgnoreCase("Coordonator") || leader.getRole().equalsIgnoreCase("Director"))) {
                            return ResponseEntity.ok(leader);
                        }
                    } else {
                        // Daca vrea sa intre doar ca Lider simplu, e ok
                        return ResponseEntity.ok(leader);
                    }
                }
            }
        }
        // Daca am ajuns aici, inseamna ca nu am gasit pe nimeni cu datele astea
        return ResponseEntity.status(401).body("Date incorecte!");
    }

    /**
     * Metoda de INREGISTRARE (Sign Up).
     * Creeaza un cont nou in baza de date.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // CAZUL 1: Inregistrare COPIL (Este simplu, nu cere cod secret)
        if ("CHILD".equalsIgnoreCase(request.getRole())) {
            Child newChild = new Child();
            // Copiem datele din formular in noul obiect Child
            newChild.setName(request.getName());
            newChild.setSurname(request.getSurname());
            newChild.setPassword(request.getPassword());
            newChild.setBirthDate(request.getBirthDate());
            newChild.setParentName(request.getParentName());
            newChild.setParentPhone(request.getParentPhone());

            // Setam valorile de start (totul pe zero la inceput de sezon)
            newChild.setSeasonPoints(0);
            newChild.setProgress(0);
            newChild.setBadgesCount(0);
            newChild.setHasManual(false);
            newChild.setHasShirt(false);
            newChild.setHasHat(false);

            childRepository.save(newChild);
            return ResponseEntity.ok("Cont COPIL creat!");
        }

        // CAZUL 2: Inregistrare LIDER (Mai complex, cere securitate)
        else {
            // PASUL 1: Securitate. Verificam codul secret (MODIFICAT: Acum nu mai citim din fisier)
            if (!isValidCode(request.getRegistrationCode())) {
                return ResponseEntity.badRequest().body("Cod de acces invalid! Cere un cod valid de la Director.");
            }

            // PASUL 2: Verificam sa nu existe deja un lider cu acest nume
            // Nota: Asigura-te ca ai metoda findByNameAndSurname in LeaderRepository!
            // Daca nu o ai, sterge if-ul acesta sau adaug-o in Repository.
            if (leaderRepository.findByNameAndSurname(request.getName(), request.getSurname()).isPresent()) {
                return ResponseEntity.badRequest().body("Lider existent!");
            }

            // PASUL 3: Cream liderul
            Leader newLeader = new Leader();
            newLeader.setName(request.getName());
            newLeader.setSurname(request.getSurname());
            newLeader.setPassword(request.getPassword());
            newLeader.setRole(request.getRole());
            newLeader.setPhoneNumber(request.getPhoneNumber());
            newLeader.setRating(0.0f); // Porneste cu nota 0

            // PASUL 4: Legam liderul de departamente (ex: Jocuri, Secretariat)
            // Primim o lista de ID-uri (ex: [1, 3]) si cautam departamentele reale in baza de date
            if (request.getDepartmentIds() != null && !request.getDepartmentIds().isEmpty()) {
                Set<Department> selectedDepts = new HashSet<>();
                for (Integer deptId : request.getDepartmentIds()) {
                    departmentRepository.findById(deptId).ifPresent(selectedDepts::add);
                }
                newLeader.setDepartments(selectedDepts);
            }

            leaderRepository.save(newLeader);
            return ResponseEntity.ok("Cont Lider creat cu succes!");
        }
    }

    /**
     * MODIFICAT: Verificam codul direct din cod, fara fisiere externe.
     * Asta rezolva problema cu "codes.txt not found".
     * Aici poti adauga sau sterge codurile acceptate.
     */
    private boolean isValidCode(String code) {
        if (code == null || code.trim().isEmpty()) return false;

        // Lista codurilor valide pentru inregistrare
        // Poti adauga aici oricate vrei
        List<String> validCodes = List.of(
                "AWANA2024",      // Cod principal
                "BETANIA",        // Alt cod
                "DIRECTOR_KEY"    // Cod pt directori
        );

        return validCodes.contains(code.trim());
    }
}