package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Aceasta clasa reprezinta o nota sau evaluare primita de un lider.
 * UPDATE: Suporta stergerea logica (Soft Delete) prin campul isVisible.
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

    @ManyToOne
    @JoinColumn(name = "leader_id")
    private Leader leader;

    @Column(name = "evaluated_by")
    private Integer evaluatedBy;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDate date;

    /**
     * Daca este TRUE, evaluarea apare in lista.
     * Daca este FALSE, inseamna ca a fost stearsa de director (dar ramane in baza de date).
     */
    @Column(name = "is_visible")
    private Boolean isVisible = true;
}