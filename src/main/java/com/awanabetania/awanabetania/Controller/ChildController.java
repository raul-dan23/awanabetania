package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Aceasta clasa se ocupa de comenzile pentru Copii.
 * Aici primim cererile de la site (React) ca sa citim lista, sa adaugam sau sa modificam copii.
 */
@RestController
@RequestMapping("/api/children")
@CrossOrigin(origins = "*") // Lasa site-ul sa se conecteze la server fara probleme
public class ChildController {

    /** Aici facem legatura cu baza de date */
    @Autowired
    private ChildRepository childRepository;

    /**
     * Trimite lista cu toti copiii catre site.
     * O folosim ca sa afisam tabelul principal cu copii.
     */
    @GetMapping
    public List<Child> getAllChildren() {
        return childRepository.findAll();
    }

    /**
     * Cauta un singur copil dupa ID-ul lui.
     * O folosim cand dam click pe un copil ca sa ii vedem profilul.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Child> getChildById(@PathVariable Integer id) {
        return childRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Adauga un copil nou in sistem.
     * Primeste datele din formularul de inscriere si le salveaza.
     */
    @PostMapping("/add")
    public Child addChild(@RequestBody Child child) {
        return childRepository.save(child);
    }

    /**
     * Modifica datele unui copil care exista deja (Edit Profile).
     * Putem schimba numele, telefonul parintilor sau daca este suspendat.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateChild(@PathVariable Integer id, @RequestBody Child childDetails) {
        // Intai cautam copilul vechi in baza de date
        Child child = childRepository.findById(id).orElse(null);

        // Daca nu exista copilul cu acest ID, dam eroare
        if(child == null) return ResponseEntity.notFound().build();

        // Daca l-am gasit, inlocuim datele vechi cu cele noi
        child.setName(childDetails.getName());
        child.setSurname(childDetails.getSurname());
        child.setParentName(childDetails.getParentName());
        child.setParentPhone(childDetails.getParentPhone());
        child.setBirthDate(childDetails.getBirthDate());

        // Aici setam daca este suspendat sau nu
        child.setIsSuspended(childDetails.getIsSuspended());

        // La final salvam modificarile in baza de date
        return ResponseEntity.ok(childRepository.save(child));
    }
}