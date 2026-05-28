package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Records an instance of a manual (handbook) issued to a child.
 * A child can have multiple manuals over time (one per season or level).
 * Status transitions: ACTIVE (currently in use) → COMPLETED (finished) or LOST.
 * This entity serves informational and statistical purposes only;
 * per-lesson progress is tracked separately in {@link ChildProgress}.
 */
@Entity
@Table(name = "child_manual")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildManual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The child who received this manual. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    @JsonIgnore
    @ToString.Exclude
    private Child child;

    /** Display name of the manual (e.g. "Manual 1"). */
    private String name;

    /** Current status: ACTIVE, COMPLETED, or LOST. See {@link ManualStatus}. */
    private String status;

    /** Date the manual was issued to the child. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Date the manual was completed; {@code null} while still active. */
    @Column(name = "end_date")
    private LocalDate endDate;
}
