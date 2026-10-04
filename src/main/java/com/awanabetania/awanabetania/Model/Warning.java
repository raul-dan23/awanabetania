package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Represents a disciplinary record (warning or suspension) issued to a child.
 * If {@code suspension} is {@code true}, the child is immediately marked suspended
 * and cannot be picked for teams. The suspension lifts automatically after
 * {@code remainingMeetings} attended meetings have elapsed (decremented on meeting close).
 */
@Entity
@Table(name = "warnings")
@Getter
@Setter
@NoArgsConstructor
public class Warning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The season this row belongs to (see {@link Season}); set by the server, never by the client. */
    @JsonIgnore
    @Column(name = "season_id")
    private Integer seasonId;

    /** Description of the incident that led to this warning. */
    @Column(name = "description")
    private String description;

    /** {@code true} if this warning carries a suspension; {@code false} for a simple note. */
    @Column(name = "suspension")
    private Boolean suspension;

    /** Number of meetings the child must still attend before the suspension is lifted. */
    @Column(name = "remaining_meetings")
    private Integer remainingMeetings;

    /** Calendar date when the incident occurred. */
    @Column(name = "date")
    private LocalDate date;

    /**
     * The child this warning belongs to.
     * Loaded lazily; excluded from JSON serialization to prevent circular references.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    @JsonIgnore
    private Child child;

    /**
     * Transient helper field used to receive the child's ID from the request body.
     * Not stored in the database; the controller resolves the full {@link Child} entity
     * from this ID before saving.
     */
    @Transient
    private Integer childId;
}
