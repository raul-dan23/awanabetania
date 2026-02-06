package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.*;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate; // <--- Aici era problema (lipsea acest import)
import java.util.ArrayList;
import java.util.HashMap;
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

    @Autowired private DepartmentRepository deptRepo;
    @Autowired private MeetingRepository meetingRepo;
    @Autowired private LeaderRepository leaderRepo;
    @Autowired private MeetingAssignmentRepository assignmentRepo;
    @Autowired private NotificationRepository notificationRepository;

    /** Listeaza toate departamentele */
    @GetMapping
    public List<Department> getAll() { return deptRepo.findAll(); }

    /** Arata cine sunt membrii permanenti ai unui departament */
    @GetMapping("/{id}/members")
    public List<Leader> getMembers(@PathVariable Integer id) {
        return leaderRepo.findByDepartmentsId(id);
    }

    /**
     * Metoda principala pentru PLANIFICARE.
     * Returneaza asignarile + eligibleLeaders (Map cu membrii fiecarui departament).
     */
    @GetMapping("/plan/{meetingId}")
    public Map<String, Object> getPlan(@PathVariable Integer meetingId) {
        Meeting meeting = meetingRepo.findById(meetingId).orElse(null);
        if (meeting == null) return null;

        // 1. Luam asignarile existente
        List<MeetingAssignment> assignments = assignmentRepo.findByMeetingId(meetingId);
        Map<Integer, List<MeetingAssignment>> groupedAssignments = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getDepartment().getId()));

        // 2. CONSTRUIM MAP-UL CU LIDERI ELIGIBILI (Membrii)
        // Cheie: ID Departament -> Valoare: Lista Lideri
        Map<Integer, List<Leader>> eligibleLeaders = new HashMap<>();

        List<Leader> allLeaders = leaderRepo.findAll();
        for (Leader leader : allLeaders) {
            for (Department dept : leader.getDepartments()) {
                eligibleLeaders.computeIfAbsent(dept.getId(), k -> new ArrayList<>()).add(leader);
            }
        }

        // 3. Trimitem totul la Frontend
        return Map.of(
                "meeting", meeting,
                "assignments", groupedAssignments,
                "directorDay", meeting.getDirectorDay() != null ? meeting.getDirectorDay() : "Neselectat",
                "eligibleLeaders", eligibleLeaders
        );
    }

    /** Metoda 1: ASIGNARE FORTATA (Directa) */
    @PostMapping("/assign")
    public ResponseEntity<?> assignDirect(@RequestBody Map<String, Integer> payload) {
        Integer meetingId = payload.get("meetingId");
        Integer deptId = payload.get("deptId");
        Integer leaderId = payload.get("leaderId");

        Meeting m = meetingRepo.findById(meetingId).orElse(null);
        Department d = deptRepo.findById(deptId).orElse(null);
        Leader l = leaderRepo.findById(leaderId).orElse(null);

        if (m != null && d != null && l != null) {
            boolean exists = assignmentRepo.findByMeetingId(meetingId).stream()
                    .anyMatch(a -> a.getLeader().getId().equals(leaderId) && a.getDepartment().getId().equals(deptId));

            if (!exists) {
                MeetingAssignment ma = new MeetingAssignment();
                ma.setMeeting(m);
                ma.setDepartment(d);
                ma.setLeader(l);
                ma.setStatus("ACCEPTED");
                assignmentRepo.save(ma);
                return ResponseEntity.ok("Asignat direct!");
            } else {
                return ResponseEntity.badRequest().body("Liderul este deja asignat aici.");
            }
        }
        return ResponseEntity.badRequest().body("Date invalide");
    }

    /** Metoda 2: NOMINALIZARE (Invitatie) */
    @PostMapping("/nominate")
    public ResponseEntity<?> nominate(@RequestBody Map<String, Integer> payload) {
        Integer meetingId = payload.get("meetingId");
        Integer deptId = payload.get("deptId");
        Integer leaderId = payload.get("leaderId");

        Meeting m = meetingRepo.findById(meetingId).orElse(null);
        Department d = deptRepo.findById(deptId).orElse(null);
        Leader l = leaderRepo.findById(leaderId).orElse(null);

        if (m != null && d != null && l != null) {
            MeetingAssignment ma = new MeetingAssignment();
            ma.setMeeting(m);
            ma.setDepartment(d);
            ma.setLeader(l);
            ma.setStatus("PENDING");
            assignmentRepo.save(ma);

            // Folosim LocalDate.now() aici - acum va merge
            String msg = String.format("📅 Ai fost propus la %s pentru data de %s.", d.getName(), m.getDate());
            Notification notif = new Notification(msg, "ALERT", String.valueOf(l.getId()), LocalDate.now());
            notificationRepository.save(notif);

            return ResponseEntity.ok("Nominalizare trimisă!");
        }
        return ResponseEntity.badRequest().build();
    }

    /** Metoda 3: RASPUNS LIDER */
    @PostMapping("/respond")
    public ResponseEntity<?> respond(@RequestBody Map<String, Object> payload) {
        Integer assignmentId = (Integer) payload.get("assignmentId");
        String response = (String) payload.get("response");

        MeetingAssignment ma = assignmentRepo.findById(assignmentId).orElse(null);
        if (ma != null) {
            if ("DECLINED".equals(response)) {
                assignmentRepo.delete(ma);
                if(ma.getDepartment().getHeadLeader() != null) {
                    // Folosim LocalDate.now() si aici
                    String msg = String.format("❌ %s %s a refuzat postul la %s.",
                            ma.getLeader().getName(), ma.getLeader().getSurname(), ma.getDepartment().getName());
                    Notification n = new Notification(msg, "INFO", String.valueOf(ma.getDepartment().getHeadLeader().getId()), LocalDate.now());
                    notificationRepository.save(n);
                }
            } else {
                ma.setStatus("ACCEPTED");
                assignmentRepo.save(ma);
            }
            return ResponseEntity.ok("Răspuns înregistrat!");
        }
        return ResponseEntity.badRequest().build();
    }

    /** Sterge o persoana din orar */
    @DeleteMapping("/remove")
    @Transactional
    public ResponseEntity<?> removeAssignment(
            @RequestParam Integer meetingId,
            @RequestParam Integer deptId,
            @RequestParam Integer leaderId) {

        assignmentRepo.deleteByMeetingIdAndDepartmentIdAndLeaderId(meetingId, deptId, leaderId);
        return ResponseEntity.ok("Șters cu succes!");
    }

    /** Seteaza Directorul de Zi */
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

    /** Seteaza Seful de Departament */
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