package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Records points awarded to a team for a single game round during a meeting.
 * Multiple entries can exist per team per meeting (one per game round).
 * The team's total game score for the evening is the sum of all related entries.
 */
@Entity
@Table(name = "team_game_points")
@Getter
@Setter
@NoArgsConstructor
public class TeamGamePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The meeting during which this game round took place. */
    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    /** Team color identifier (e.g. "red", "blue", "green", "yellow"). */
    @Column(name = "team_name")
    private String teamColor;

    /** Points awarded for this round. */
    private Integer points;
}
