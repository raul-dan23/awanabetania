package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Meeting;
import com.awanabetania.awanabetania.Model.Score;
import com.awanabetania.awanabetania.Model.Warning;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.MeetingRepository;
import com.awanabetania.awanabetania.Repository.ScoreRepository;
import com.awanabetania.awanabetania.Repository.WarningRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Aceasta clasa se ocupa de serile de club (Intalniri).
 * Aici pornim o seara noua si, cel mai important, apasam butonul de Final de Seara.
 */
@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "*")
public class MeetingController {

    @Autowired private MeetingRepository meetingRepository;
    @Autowired private WarningRepository warningRepository;
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private ChildRepository childRepository;

    /**
     * Arata doar intalnirile care urmeaza sau sunt in desfasurare (cele neterminate).
     */
    @GetMapping
    public List<Meeting> getUpcomingMeetings() {
        return meetingRepository.findByIsCompletedFalseOrderByDateAsc();
    }

    /**
     * Programeaza o noua seara de club in calendar.
     */
    @PostMapping("/add")
    public Meeting addMeeting(@RequestBody Meeting meeting) {
        return meetingRepository.save(meeting);
    }

    /**
     * --- METODA CRITICA: INCHEIEREA SERII ---
     * Aceasta metoda se apeleaza cand Directorul apasa "Inchide Seara".
     * * Face 3 lucruri:
     * 1. Marcheaza seara ca terminata in Calendar.
     * 2. Scade pedepsele copiilor (daca au fost prezenti).
     * 3. RESETEAZA echipele si punctele zilnice ale copiilor.
     */
    @PostMapping("/close/{id}")
    @Transactional // Siguranta: daca ceva crapa, nu se salveaza nimic
    public ResponseEntity<?> closeMeeting(@PathVariable Integer id) {

        // Pasul 0: Gasim intalnirea si o marcam ca terminata
        Meeting meeting = meetingRepository.findById(id).orElse(null);
        if (meeting == null) return ResponseEntity.badRequest().body("Eroare ID");

        meeting.setIsCompleted(true);
        meetingRepository.save(meeting);

        // --- PASUL 1: GESTIONARE PEDEPSE (SUSPENDARI) ---

        // Luam toate scorurile de la ACEASTA intalnire ca sa vedem cine a fost prezent
        List<Score> scoresToday = scoreRepository.findAll().stream()
                .filter(s -> s.getMeeting().getId().equals(id))
                .toList();

        // Facem o lista cu ID-urile copiilor prezenti
        List<Integer> presentChildIds = scoresToday.stream()
                .map(score -> score.getChild().getId())
                .distinct()
                .collect(Collectors.toList());

        // Luam lista cu copiii care au pedepse active
        List<Warning> activeWarnings = warningRepository.findAll().stream()
                .filter(w -> Boolean.TRUE.equals(w.getSuspension()) && w.getRemainingMeetings() > 0)
                .collect(Collectors.toList());

        // Verificam fiecare pedepsit:
        for (Warning w : activeWarnings) {
            // Daca copilul pedepsit a fost punctat azi (deci a fost prezent)
            if (presentChildIds.contains(w.getChild().getId())) {
                w.setRemainingMeetings(w.getRemainingMeetings() - 1);

                // Daca pedeapsa a ajuns la 0, il eliberam
                if (w.getRemainingMeetings() == 0) {
                    w.setSuspension(false);

                    // Il deblocam si in tabelul de copii
                    Child c = w.getChild();
                    c.setIsSuspended(false);
                    childRepository.save(c);
                }
                warningRepository.save(w);
            }
        }

        // --- PASUL 2: CURATENIE GENERALA (RESET) ---
        // Aici este marea schimbare. Nu stergem tabele, ci resetam campurile copiilor.

        List<Child> allChildren = childRepository.findAll();
        for (Child c : allChildren) {
            // 1. Punctele de azi (care s-au dus la echipa) devin 0.
            c.setDailyPoints(0);

            // 2. Copilul iese din echipa (devine disponibil pt data viitoare).
            c.setCurrentTeam(null);
        }

        // Salvam toti copiii deodata (foarte rapid)
        childRepository.saveAll(allChildren);

        return ResponseEntity.ok("✅ Intalnire incheiata! Echipele si punctele zilei au fost resetate.");
    }
}