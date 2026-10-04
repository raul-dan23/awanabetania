package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Represents a performance evaluation given to a leader after a club meeting.
 * Evaluations support soft deletion: setting {@code isVisible} to {@code false}
 * hides the record without removing it from the database. The leader's average
 * {@code rating} is recalculated each time an evaluation is saved or soft-deleted,
 * counting only visible evaluations.
 */
@Entity
@Table(name = "leader_evaluations")
@Getter
@Setter
@NoArgsConstructor
public class LeaderEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The season this row belongs to (see {@link Season}); set by the server, never by the client. */
    @JsonIgnore
    @Column(name = "season_id")
    private Integer seasonId;

    /** The leader who received this evaluation. */
    @ManyToOne
    @JoinColumn(name = "leader_id")
    private Leader leader;

    /** ID of the leader (typically the director) who submitted this evaluation. */
    @Column(name = "evaluated_by")
    private Integer evaluatedBy;

    /** Numeric score given in this evaluation. */
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDate date;

    /**
     * Visibility flag for soft deletion.
     * {@code true} while the evaluation is active; {@code false} after the director hides it.
     */
    @Column(name = "is_visible")
    private Boolean isVisible = true;
}
