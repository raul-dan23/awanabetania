package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Records the point breakdown for a single child at a single club meeting.
 * Each scoring criterion (attendance, bible, handbook, lesson, friend, uniform, extra)
 * is stored as a boolean or integer. The computed total is persisted in {@code total}.
 * A human-readable summary of which criteria were met is stored in {@code details}.
 */
@Entity
@Table(name = "scores")
@Getter
@Setter
@NoArgsConstructor
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The season this row belongs to (see {@link Season}); set by the server, never by the client. */
    @JsonIgnore
    @Column(name = "season_id")
    private Integer seasonId;

    @ManyToOne
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    private Boolean attended;
    private Boolean hasBible;
    private Boolean hasHandbook;
    private Boolean hasUniform;
    private Boolean lesson;
    private Boolean friend;
    private Integer extraPoints;

    /** Points attributed to this child's individual performance (same as total for now). */
    @Column(name = "individual_points")
    private Integer individualPoints = 0;

    /** Points contributed toward the child's team score for today. */
    @Column(name = "team_points")
    private Integer teamPoints = 0;

    /** Total points earned at this meeting (sum of all individual criteria + extra). */
    private Integer total = 0;

    @Column(nullable = false)
    private LocalDate date;

    /** Comma-separated list of criteria that were fulfilled (e.g. "Prezenta, Biblie, Lectie"). */
    @Column(columnDefinition = "TEXT")
    private String details;
}
