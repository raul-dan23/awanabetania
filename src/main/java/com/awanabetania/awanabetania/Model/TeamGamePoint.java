package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * (ACTUALIZAT)
 * Aceasta clasa reprezinta punctele castigate de o echipa la jocuri.
 * Acum este legata de INTALNIRE (Meeting), nu doar de data, pentru precizie.
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

    /** Legatura cu seara specifica de club */
    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    /** Numele echipei (red, blue, green, yellow) */
    @Column(name = "team_name")
    private String teamColor;

    /** Punctele castigate (ex: 1000) */
    private Integer points;
}