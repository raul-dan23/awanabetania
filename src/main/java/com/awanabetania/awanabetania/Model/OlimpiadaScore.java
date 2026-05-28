package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Records a single team's result for one round submitted by one arbiter.
 * Four rows are saved per round submission (one per team).
 * A {@code place} of 0 and a {@code roundNumber} of 0 indicate an extra-points entry
 * (bonus points without placement, added via the separate extra endpoint).
 */
@Entity
@Table(name = "olimpiada_scores")
@Getter @Setter @NoArgsConstructor
public class OlimpiadaScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id")
    private Integer sessionId;

    /** Sequential round number (1, 2, 3, …). 0 is reserved for extra-points entries. */
    @Column(name = "round_number")
    private Integer roundNumber;

    /** Team name: ROSU, GALBEN, ALBASTRU, or VERDE. */
    private String team;

    /** Name of the arbiter who submitted this round. */
    @Column(name = "arbiter_name")
    private String arbiterName;

    /** Finishing position (1–4). 0 for extra-points entries. */
    private Integer place;

    /** Points awarded. For regular rounds: derived from place via BASE_POINTS; doubled if {@code isDouble}. */
    private Integer points;

    /** {@code true} if this round was played with doubled point values. */
    @Column(name = "is_double")
    private Boolean isDouble = false;

    /** Optional free-text note for extra-points entries. */
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
