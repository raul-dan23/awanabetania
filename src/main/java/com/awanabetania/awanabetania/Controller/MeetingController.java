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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "*")
public class MeetingController {

    @Autowired private MeetingRepository meetingRepository;
    @Autowired private WarningRepository warningRepository;
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private ChildRepository childRepository;

    @GetMapping
    public List<Meeting> getUpcomingMeetings() {
        return meetingRepository.findByIsCompletedFalseOrderByDateAsc();
    }

    @PostMapping("/add")
    public Meeting addMeeting(@RequestBody Meeting meeting) {
        return meetingRepository.save(meeting);
    }

    /**
     * --- INCHEIEREA SERII ---
     * 1. Marcheaza intalnirea terminata.
     * 2. Scade pedepsele.
     * 3. Reseteaza echipele.
     * 4. RESETEAZA STREAK-UL PENTRU ABSENTI.
     */
    @PostMapping("/close/{id}")
    @Transactional
    public ResponseEntity<?> closeMeeting(@PathVariable Integer id) {

        Meeting meeting = meetingRepository.findById(id).orElse(null);
        if (meeting == null) return ResponseEntity.badRequest().body("Eroare ID");

        meeting.setIsCompleted(true);
        meetingRepository.save(meeting);

        // 1. Gestionare Pedepse (doar pentru cei prezenti la jocuri/intalnire)
        List<Score> scoresToday = scoreRepository.findAll().stream().filter(s -> s.getMeeting().getId().equals(id)).toList();
        List<Integer> presentChildIds = scoresToday.stream().map(s -> s.getChild().getId()).distinct().toList();
        List<Warning> activeWarnings = warningRepository.findAll().stream().filter(w -> Boolean.TRUE.equals(w.getSuspension()) && w.getRemainingMeetings() > 0).toList();

        for (Warning w : activeWarnings) {
            if (presentChildIds.contains(w.getChild().getId())) {
                w.setRemainingMeetings(w.getRemainingMeetings() - 1);
                if (w.getRemainingMeetings() == 0) {
                    w.setSuspension(false);
                    Child c = w.getChild(); c.setIsSuspended(false); childRepository.save(c);
                }
                warningRepository.save(w);
            }
        }

        // 2. CURATENIE GENERALA & RESETARE ABSENTI
        LocalDate today = LocalDate.now();
        List<Child> allChildren = childRepository.findAll();
        int absentsReset = 0;

        for (Child c : allChildren) {
            // Reset echipe si puncte zilnice (pentru toti)
            c.setDailyPoints(0);
            c.setCurrentTeam(null);

            // LOGICA TA: Verificam cine a fost prezent AZI
            // Daca lastAttendanceDate NU este azi, inseamna ca a lipsit la aceasta intalnire.
            boolean wasPresentToday = (c.getLastAttendanceDate() != null && c.getLastAttendanceDate().equals(today));

            if (!wasPresentToday) {
                // A lipsit! Ii resetam streak-ul la 0.
                if (c.getAttendanceStreak() != null && c.getAttendanceStreak() > 0) {
                    c.setAttendanceStreak(0);
                    absentsReset++;
                }
            }
        }

        childRepository.saveAll(allChildren);

        return ResponseEntity.ok("✅ Intalnire incheiata! " + absentsReset + " absenti au fost resetati la 0.");
    }
}