package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages department structures and per-meeting leader assignments.
 * The planning flow supports two assignment modes:
 * <ul>
 *   <li><b>Direct assignment</b> — the leader is immediately set to ACCEPTED status.
 *       If the department is Secretariat, a 4-digit PIN is generated for the meeting
 *       (once, on first assignment) and sent to the leader as a notification.</li>
 *   <li><b>Nomination</b> — creates a PENDING assignment and sends the leader a notification.
 *       The leader then responds (accept keeps the assignment as ACCEPTED; decline deletes it
 *       and notifies the department head).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired private DepartmentRepository deptRepo;
    @Autowired private MeetingRepository meetingRepo;
    @Autowired private LeaderRepository leaderRepo;
    @Autowired private MeetingAssignmentRepository assignmentRepo;
    @Autowired private NotificationRepository notificationRepository;

    /**
     * Returns all departments.
     *
     * @return list of all {@link Department} entities
     */
    @GetMapping
    public List<Department> getAll() {
        return deptRepo.findAll();
    }

    /**
     * Returns all leaders who are permanent members of the given department.
     *
     * @param id the department's primary key
     * @return list of member leaders
     */
    @GetMapping("/{id}/members")
    public List<Leader> getMembers(@PathVariable Integer id) {
        return leaderRepo.findByDepartmentsId(id);
    }

    /**
     * Returns the full planning data for a meeting: existing assignments grouped by department,
     * and a map of eligible leaders (permanent members) per department.
     *
     * @param meetingId the meeting's primary key
     * @return map with keys "meeting", "assignments", "directorDay", "eligibleLeaders";
     *         or {@code null} if the meeting does not exist
     */
    @GetMapping("/plan/{meetingId}")
    public Map<String, Object> getPlan(@PathVariable Integer meetingId) {
        Meeting meeting = meetingRepo.findById(meetingId).orElse(null);
        if (meeting == null) return null;

        List<MeetingAssignment> assignments = assignmentRepo.findByMeetingId(meetingId);
        Map<Integer, List<MeetingAssignment>> groupedAssignments = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getDepartment().getId()));

        // Build eligible-leaders map: departmentId → list of member leaders
        Map<Integer, List<Leader>> eligibleLeaders = new HashMap<>();
        List<Leader> allLeaders = leaderRepo.findAll();
        for (Leader leader : allLeaders) {
            for (Department dept : leader.getDepartments()) {
                eligibleLeaders.computeIfAbsent(dept.getId(), k -> new ArrayList<>()).add(leader);
            }
        }

        return Map.of(
                "meeting", meeting,
                "assignments", groupedAssignments,
                "directorDay", meeting.getDirectorDay() != null ? meeting.getDirectorDay() : "Unassigned",
                "eligibleLeaders", eligibleLeaders
        );
    }

    /**
     * Directly assigns a leader to a department for a meeting (status: ACCEPTED).
     * If the department is Secretariat and no PIN exists for the meeting yet,
     * a 4-digit PIN is generated and sent to the assigned leader via notification.
     *
     * @param payload JSON with keys "meetingId", "deptId", "leaderId"
     * @return 200 on success; 400 if the leader is already assigned or IDs are invalid
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assignDirect(@RequestBody Map<String, Integer> payload) {
        Integer meetingId = payload.get("meetingId");
        Integer deptId = payload.get("deptId");
        Integer leaderId = payload.get("leaderId");

        Meeting m = meetingRepo.findById(meetingId).orElse(null);
        Department d = deptRepo.findById(deptId).orElse(null);
        Leader l = leaderRepo.findById(leaderId).orElse(null);

        if (m == null || d == null || l == null) return ResponseEntity.badRequest().body("Invalid data.");

        boolean exists = assignmentRepo.findByMeetingId(meetingId).stream()
                .anyMatch(a -> a.getLeader() != null &&
                               a.getLeader().getId().equals(leaderId) &&
                               a.getDepartment().getId().equals(deptId));
        if (exists) return ResponseEntity.badRequest().body("Leader is already assigned here.");

        MeetingAssignment ma = new MeetingAssignment();
        ma.setMeeting(m);
        ma.setDepartment(d);
        ma.setLeader(l);
        ma.setStatus("ACCEPTED");
        assignmentRepo.save(ma);

        // Generate a PIN the first time a secretariat leader is assigned to this meeting
        if (d.getName().toLowerCase().contains("secretar")) {
            if (m.getMeetingPin() == null) {
                String pinCode = String.valueOf(1000 + new java.security.SecureRandom().nextInt(9000));
                m.setMeetingPin(pinCode);
                meetingRepo.save(m);
            }

            Notification n = new Notification();
            n.setTitle("SECRETARIAT ACCESS CODE");
            n.setMessage("You have been assigned to Secretariat.\n\nThe PIN for scoring is: " + m.getMeetingPin() + "\n\nDo not share it with the children!");
            n.setDate(LocalDate.now());
            n.setType("INFO");
            n.setVisibleTo(String.valueOf(l.getId()));
            notificationRepository.save(n);
        }

        return ResponseEntity.ok("Assigned directly.");
    }

    /**
     * Nominates a leader for a department slot (status: PENDING) and sends them a notification.
     *
     * @param payload JSON with keys "meetingId", "deptId", "leaderId"
     * @return 200 on success; 400 if IDs are invalid
     */
    @PostMapping("/nominate")
    public ResponseEntity<?> nominate(@RequestBody Map<String, Integer> payload) {
        Integer meetingId = payload.get("meetingId");
        Integer deptId = payload.get("deptId");
        Integer leaderId = payload.get("leaderId");

        Meeting m = meetingRepo.findById(meetingId).orElse(null);
        Department d = deptRepo.findById(deptId).orElse(null);
        Leader l = leaderRepo.findById(leaderId).orElse(null);

        if (m == null || d == null || l == null) return ResponseEntity.badRequest().build();

        MeetingAssignment ma = new MeetingAssignment();
        ma.setMeeting(m);
        ma.setDepartment(d);
        ma.setLeader(l);
        ma.setStatus("PENDING");
        assignmentRepo.save(ma);

        String msg = String.format("You have been nominated for %s on %s.", d.getName(), m.getDate());
        notificationRepository.save(new Notification(msg, "ALERT", String.valueOf(l.getId()), LocalDate.now()));

        return ResponseEntity.ok("Nomination sent.");
    }

    /**
     * Records a leader's response to a nomination.
     * Accepting sets the status to ACCEPTED; declining deletes the assignment and
     * notifies the department head.
     *
     * @param payload JSON with keys "assignmentId" (Integer) and "response" (String: "ACCEPTED" or "DECLINED")
     * @return 200 on success; 400 if the assignment is not found
     */
    @PostMapping("/respond")
    public ResponseEntity<?> respond(@RequestBody Map<String, Object> payload) {
        Integer assignmentId = (Integer) payload.get("assignmentId");
        String response = (String) payload.get("response");

        MeetingAssignment ma = assignmentRepo.findById(assignmentId).orElse(null);
        if (ma == null) return ResponseEntity.badRequest().build();

        if ("DECLINED".equals(response)) {
            assignmentRepo.delete(ma);
            if (ma.getDepartment().getHeadLeader() != null) {
                String msg = String.format("%s %s declined the slot at %s.",
                        ma.getLeader().getName(), ma.getLeader().getSurname(), ma.getDepartment().getName());
                notificationRepository.save(new Notification(
                        msg, "INFO", String.valueOf(ma.getDepartment().getHeadLeader().getId()), LocalDate.now()));
            }
        } else {
            ma.setStatus("ACCEPTED");
            assignmentRepo.save(ma);
        }

        return ResponseEntity.ok("Response recorded.");
    }

    /**
     * Removes a leader from a specific department slot in the meeting schedule.
     *
     * @param meetingId  the meeting's primary key
     * @param deptId     the department's primary key
     * @param leaderId   the leader's primary key
     * @return 200 on success
     */
    @DeleteMapping("/remove")
    @Transactional
    public ResponseEntity<?> removeAssignment(
            @RequestParam Integer meetingId,
            @RequestParam Integer deptId,
            @RequestParam Integer leaderId) {
        assignmentRepo.deleteByMeetingIdAndDepartmentIdAndLeaderId(meetingId, deptId, leaderId);
        return ResponseEntity.ok("Removed successfully.");
    }

    /**
     * Sets the director-of-the-day for a meeting.
     *
     * @param meetingId the meeting's primary key
     * @param leaderId  the leader's primary key (passed as the request body)
     * @return 200 on success; 400 if IDs are invalid
     */
    @PostMapping("/director/{meetingId}")
    public ResponseEntity<?> setMeetingDirector(@PathVariable Integer meetingId, @RequestBody Integer leaderId) {
        Meeting m = meetingRepo.findById(meetingId).orElse(null);
        Leader l = leaderRepo.findById(leaderId).orElse(null);
        if (m == null || l == null) return ResponseEntity.badRequest().body("Invalid data.");
        m.setDirectorDay(l);
        meetingRepo.save(m);
        return ResponseEntity.ok("Director set.");
    }

    /**
     * Sets the head leader (department manager) for a department.
     *
     * @param id       the department's primary key
     * @param leaderId the leader's primary key (passed as the request body)
     * @return 200 on success; 400 if IDs are invalid
     */
    @PostMapping("/{id}/set-head")
    public ResponseEntity<?> setDepartmentHead(@PathVariable Integer id, @RequestBody Integer leaderId) {
        Department dept = deptRepo.findById(id).orElse(null);
        Leader leader = leaderRepo.findById(leaderId).orElse(null);
        if (dept == null || leader == null) return ResponseEntity.badRequest().body("Invalid data.");
        dept.setHeadLeader(leader);
        deptRepo.save(dept);
        return ResponseEntity.ok("Department head updated.");
    }
}
