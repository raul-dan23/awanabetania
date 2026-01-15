package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.*;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aceasta clasa se ocupa de organizarea departamentelor si planificarea serilor.
 * Aici facem orarul: cine unde se implica, cine este director de zi si trimitem invitatii.
 */
@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    // Avem nevoie de acces la aproape toate tabelele ca sa facem legaturile
    @Autowired private DepartmentRepository deptRepo;
    @Autowired private MeetingRepository meetingRepo;
    @Autowired private LeaderRepository leaderRepo;
    @Autowired private MeetingAssignmentRepository assignmentRepo;
    @Autowired private NotificationRepository notificationRepository;

    /** Listeaza toate departamentele (Jocuri, Secretariat, etc.) */
    @GetMapping
    public List<Department> getAll() { return deptRepo.findAll(); }

    /** Arata cine sunt membrii permanenti ai unui departament */
    @GetMapping("/{id}/members")
    public List<Leader> getMembers(@PathVariable Integer id) {
        // Cautam liderii care fac parte din acest departament (Join automat prin JPA)
        return leaderRepo.findByDepartmentsId(id);
    }

    /**
     * Metoda principala pentru PLANIFICARE.
     * Returneaza toate datele necesare pentru a desena tabelul cu orarul unei seri.
     * Grupeaza liderii pe departamente ca sa arate frumos in pagina.
     */
    @GetMapping("/plan/{meetingId}")
    public Map<String, Object> getPlan(@PathVariable Integer meetingId) {
        Meeting meeting = meetingRepo.findById(meetingId).orElse(null);
        if (meeting == null) return null;

        // Luam lista bruta de asignari din baza de date pentru aceasta sedinta
        List<MeetingAssignment> assignments = assignmentRepo.findByMeetingId(meetingId);

        // O grupam pe categorii (Cheia este ID-ul departamentului)
        // Rezultat: { 1: [AsignareLiderA, AsignareLiderB], 2: [AsignareLiderC] }
        Map<Integer, List<MeetingAssignment>> grouped = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getDepartment().getId()));

        // Trimitem pachetul complet catre site
        return Map.of(
                "meeting", meeting,
                "assignments", grouped,
                "directorDay", meeting.getDirectorDay() != null ? meeting.getDirectorDay() : "Neselectat"
        );
    }

    /**
     * Metoda 1: ASIGNARE FORTATA (Directa).
     * Directorul pune un lider pe post direct, fara sa il mai intrebe.
     * Statusul devine automat "ACCEPTED".
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assignDirect(@RequestBody Map<String, Integer> payload) {
        Integer meetingId = payload.get("meetingId");
        Integer deptId = payload.get("deptId");
        Integer leaderId = payload.get("leaderId");

        Meeting m = meetingRepo.findById(meetingId).orElse(null);
        Department d = deptRepo.findById(deptId).orElse(null);
        Leader l = leaderRepo.findById(leaderId).orElse(null);

        if (m != null && d != null && l != null) {
            // Verificam sa nu fie deja pus, ca sa nu avem duplicate
            boolean exists = assignmentRepo.findByMeetingId(meetingId).stream()
                    .anyMatch(a -> a.getLeader().getId().equals(leaderId) && a.getDepartment().getId().equals(deptId));

            if (!exists) {
                MeetingAssignment ma = new MeetingAssignment();
                ma.setMeeting(m);
                ma.setDepartment(d);
                ma.setLeader(l);
                ma.setStatus("ACCEPTED"); // Direct acceptat
                assignmentRepo.save(ma);
                return ResponseEntity.ok("Asignat direct!");
            } else {
                return ResponseEntity.badRequest().body("Liderul este deja asignat aici.");
            }
        }
        return ResponseEntity.badRequest().body("Date invalide");
    }

    /**
     * Metoda 2: NOMINALIZARE (Invitatie).
     * Directorul propune un lider, dar liderul trebuie sa accepte.
     * Trimitem o notificare liderului si punem statusul "PENDING" (in asteptare).
     */
    @PostMapping("/nominate")
    public ResponseEntity<?> nominate(@RequestBody Map<String, Integer> payload) {
        Integer meetingId = payload.get("meetingId");
        Integer deptId = payload.get("deptId");
        Integer leaderId = payload.get("leaderId");

        Meeting m = meetingRepo.findById(meetingId).orElse(null);
        Department d = deptRepo.findById(deptId).orElse(null);
        Leader l = leaderRepo.findById(leaderId).orElse(null);

        if (m != null && d != null && l != null) {
            // Cream cererea in asteptare
            MeetingAssignment ma = new MeetingAssignment();
            ma.setMeeting(m);
            ma.setDepartment(d);
            ma.setLeader(l);
            ma.setStatus("PENDING");
            assignmentRepo.save(ma);

            // Trimitem Alerta pe telefonul liderului
            String msg = String.format("📅 Ai fost propus la %s pentru data de %s.", d.getName(), m.getDate());
            Notification notif = new Notification(msg, "ALERT", String.valueOf(l.getId()), LocalDate.now());
            notificationRepository.save(notif);

            return ResponseEntity.ok("Nominalizare trimisă!");
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Metoda 3: RASPUNS LIDER.
     * Liderul apasa pe butonul Accept sau Refuz din notificare.
     * Daca refuza, stergem asignarea si anuntam seful de departament.
     */
    @PostMapping("/respond")
    public ResponseEntity<?> respond(@RequestBody Map<String, Object> payload) {
        Integer assignmentId = (Integer) payload.get("assignmentId");
        String response = (String) payload.get("response");

        MeetingAssignment ma = assignmentRepo.findById(assignmentId).orElse(null);
        if (ma != null) {
            if ("DECLINED".equals(response)) {
                assignmentRepo.delete(ma); // Il stergem din orar

                // Anuntam seful departamentului ca a ramas fara om
                if(ma.getDepartment().getHeadLeader() != null) {
                    String msg = String.format("❌ %s %s a refuzat postul la %s.",
                            ma.getLeader().getName(), ma.getLeader().getSurname(), ma.getDepartment().getName());
                    Notification n = new Notification(msg, "INFO", String.valueOf(ma.getDepartment().getHeadLeader().getId()), LocalDate.now());
                    notificationRepository.save(n);
                }
            } else {
                ma.setStatus("ACCEPTED"); // A acceptat, ramane in tabel
                assignmentRepo.save(ma);
            }
            return ResponseEntity.ok("Răspuns înregistrat!");
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Sterge o persoana din orar (butonul de gunoi).
     * Transactional este necesar cand facem stergeri custom.
     */
    @DeleteMapping("/remove")
    @Transactional
    public ResponseEntity<?> removeAssignment(
            @RequestParam Integer meetingId,
            @RequestParam Integer deptId,
            @RequestParam Integer leaderId) {

        assignmentRepo.deleteByMeetingIdAndDepartmentIdAndLeaderId(meetingId, deptId, leaderId);
        return ResponseEntity.ok("Șters cu succes!");
    }

    /** Seteaza cine este Directorul de Zi (cel care conduce seara) */
    @PostMapping("/director/{meetingId}")
    public ResponseEntity<?> setMeetingDirector(@PathVariable Integer meetingId, @RequestBody Integer leaderId) {
        Meeting m = meetingRepo.findById(meetingId).orElse(null);
        Leader l = leaderRepo.findById(leaderId).orElse(null);

        if (m != null && l != null) {
            m.setDirectorDay(l);
            meetingRepo.save(m);
            return ResponseEntity.ok("Director setat!");
        }
        return ResponseEntity.badRequest().body("Eroare");
    }

    /** Seteaza cine este Seful unui Departament (pe tot anul) */
    @PostMapping("/{id}/set-head")
    public ResponseEntity<?> setDepartmentHead(@PathVariable Integer id, @RequestBody Integer leaderId) {
        Department dept = deptRepo.findById(id).orElse(null);
        Leader leader = leaderRepo.findById(leaderId).orElse(null);
        if (dept != null && leader != null) {
            dept.setHeadLeader(leader);
            deptRepo.save(dept);
            return ResponseEntity.ok("Actualizat!");
        }
        return ResponseEntity.badRequest().body("Eroare.");
    }
}