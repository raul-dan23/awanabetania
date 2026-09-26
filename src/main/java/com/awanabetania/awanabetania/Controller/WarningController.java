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
 * Handles disciplinary records (warnings and suspensions) for children.
 * When a suspension is created, the child's {@code isSuspended} flag is set immediately
 * so the team-selection screen reflects the change in real time.
 * Suspensions are automatically lifted by {@code MeetingController} when the
 * {@code remainingMeetings} counter reaches zero.
 */
@RestController
@RequestMapping("/api/warnings")
public class WarningController {

    @Autowired
    private WarningRepository warningRepository;

    @Autowired
    private ChildRepository childRepository;

    /**
     * Returns all warnings for a given child, ordered newest first.
     *
     * @param childId the child's primary key
     * @return list of {@link Warning} records
     */
    @GetMapping("/child/{childId}")
    public List<Warning> getWarnings(@PathVariable Integer childId) {
        return warningRepository.findByChildIdOrderByIdDesc(childId);
    }

    /**
     * Creates a new warning or suspension for a child.
     * If {@code suspension} is {@code true}, the child's profile is immediately flagged
     * as suspended in the database.
     *
     * @param warningRequest warning data including {@code childId}, {@code description},
     *                       {@code suspension}, and {@code remainingMeetings}
     * @return 200 on success; 400 if the child does not exist
     */
    @PostMapping("/add")
    public ResponseEntity<?> addWarning(@RequestBody Warning warningRequest) {
        Child child = childRepository.findById(warningRequest.getChildId()).orElse(null);
        if (child == null) return ResponseEntity.badRequest().body("Child not found.");

        Warning warning = new Warning();
        warning.setChild(child);
        warning.setDescription(warningRequest.getDescription());
        warning.setSuspension(warningRequest.getSuspension());
        warning.setRemainingMeetings(warningRequest.getRemainingMeetings());
        warning.setDate(LocalDate.now());
        warningRepository.save(warning);

        if (Boolean.TRUE.equals(warningRequest.getSuspension())) {
            child.setIsSuspended(true);
            childRepository.save(child);
        }

        return ResponseEntity.ok("Disciplinary record saved and profile updated.");
    }
}
