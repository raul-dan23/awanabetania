package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Warning;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.WarningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Aceasta clasa se ocupa de disciplina si pedepse.
 * Aici dam avertismente si suspendam copiii care au incalcat regulile.
 */
@RestController
@RequestMapping("/api/warnings")
@CrossOrigin(origins = "*")
public class WarningController {

    // Avem nevoie de acces la tabelele de Avertismente si Copii
    @Autowired
    private WarningRepository warningRepository;

    @Autowired
    private ChildRepository childRepository;

    /**
     * Arata lista cu toate pedepsele primite de un copil in trecut.
     * Folosita in pagina de profil ca sa vedem istoricul disciplinar.
     */
    @GetMapping("/child/{childId}")
    public List<Warning> getWarnings(@PathVariable Integer childId) {
        return warningRepository.findByChildIdOrderByIdDesc(childId);
    }

    /**
     * Adauga o pedeapsa noua.
     * Aceasta metoda face doua lucruri importante:
     * 1. Salveaza pedeapsa in istoric.
     * 2. Daca e vorba de suspendare, blocheaza imediat copilul in sistem.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addWarning(@RequestBody Warning warningRequest) {

        // Pasul 1: Cautam copilul caruia ii dam avertismentul
        Child child = childRepository.findById(warningRequest.getChildId()).orElse(null);
        if (child == null) return ResponseEntity.badRequest().body("Copil inexistent");

        // Pasul 2: Cream fisa de pedeapsa si scriem motivul (de ce e pedepsit)
        Warning warning = new Warning();
        warning.setChild(child);
        warning.setDescription(warningRequest.getDescription());
        warning.setSuspension(warningRequest.getSuspension());
        warning.setRemainingMeetings(warningRequest.getRemainingMeetings());
        warning.setDate(LocalDate.now());

        // Salvam pedeapsa in baza de date
        warningRepository.save(warning);

        // Pasul 3: SINCRONIZARE IMPORTANTA
        // Daca pedeapsa este "Suspendare", trebuie sa blocam copilul imediat in sistem.
        // Astfel, el va aparea cu Rosu si nu va putea fi ales in echipe la jocuri.
        if (Boolean.TRUE.equals(warningRequest.getSuspension())) {
            child.setIsSuspended(true);
            childRepository.save(child);
        }

        return ResponseEntity.ok("Sanctiune inregistrata si profil actualizat!");
    }
}