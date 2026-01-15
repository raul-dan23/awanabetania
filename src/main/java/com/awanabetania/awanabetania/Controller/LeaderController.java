package com.awanabetania.awanabetania.Controller;
import com.awanabetania.awanabetania.Model.Department;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Aceasta clasa gestioneaza conturile liderilor.
 * Aici putem vedea cine sunt liderii si le putem modifica datele (Edit Profile).
 */
@RestController
@RequestMapping("/api/leaders")
@CrossOrigin(origins = "*")
public class LeaderController {

    @Autowired
    private LeaderRepository leaderRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * Trimite lista completa cu toti liderii catre site.
     * O folosim in pagina "Echipa" sau la selectarea directorului de zi.
     */
    @GetMapping
    public List<Leader> getAllLeaders() {
        return leaderRepository.findAll();
    }

    /**
     * Cauta un singur lider dupa ID.
     * O folosim cand intram pe pagina de profil a cuiva.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Leader> getLeaderById(@PathVariable Integer id) {
        return leaderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Actualizeaza (modifica) datele unui lider.
     * Este o metoda complexa pentru ca trebuie sa fim atenti la Parola si Departamente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Leader> updateLeader(@PathVariable Integer id, @RequestBody Leader leaderDetails) {
        // Cautam liderul in baza de date
        return leaderRepository.findById(id)
                .map(leader -> {
                    // 1. Actualizam datele simple (Nume, Telefon, Notite)
                    leader.setName(leaderDetails.getName());
                    leader.setSurname(leaderDetails.getSurname());
                    leader.setPhoneNumber(leaderDetails.getPhoneNumber());
                    leader.setNotes(leaderDetails.getNotes());

                    // 2. LOGICA PAROLA: O schimbam DOAR daca a scris ceva nou.
                    // Daca a lasat casuta goala, inseamna ca nu vrea sa o schimbe, deci o pastram pe cea veche.
                    if (leaderDetails.getPassword() != null && !leaderDetails.getPassword().trim().isEmpty()) {
                        leader.setPassword(leaderDetails.getPassword());
                    }

                    // 3. LOGICA DEPARTAMENTE: Aici e mai delicat.
                    if (leaderDetails.getDepartments() != null) {
                        // Pasul A: Stergem toate departamentele pe care le avea inainte (resetare).
                        leader.getDepartments().clear();

                        // Pasul B: Adaugam departamentele noi bifate in formular.
                        // Le cautam in baza de date ca sa fim siguri ca exista.
                        for (Department d : leaderDetails.getDepartments()) {
                            departmentRepository.findById(d.getId()).ifPresent(fetchedDept -> {
                                leader.getDepartments().add(fetchedDept);
                            });
                        }
                    }

                    // La final salvam totul
                    Leader updatedLeader = leaderRepository.save(leader);
                    return ResponseEntity.ok(updatedLeader);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}