package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Aceasta clasa reprezinta fisa de punctaj a unui copil pentru o singura seara.
 * Folosim Lombok (@Getter, @Setter) pentru a scapa de codul repetitiv.
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

    @Column(name = "individual_points")
    private Integer individualPoints = 0;

    @Column(name = "team_points")
    private Integer teamPoints = 0;

    /** Totalul punctelor pe ziua respectiva */
    private Integer total = 0;

    /** Data calendaristica */
    @Column(nullable = false)
    private LocalDate date;

    /** Explicatii text (ex: "Prezenta, Uniforma") */
    @Column(columnDefinition = "TEXT")
    private String details;
}