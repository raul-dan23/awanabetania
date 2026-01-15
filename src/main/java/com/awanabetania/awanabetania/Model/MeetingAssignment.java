package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aceasta clasa face legatura dintre un lider, un departament si o seara de club.
 * Aici tinem minte cine unde lucreaza si daca a acceptat postul.
 */
@Entity
@Table(name = "meeting_assignments")
@Getter
@Setter
@NoArgsConstructor
public class MeetingAssignment {

    /** ID unic pentru aceasta planificare */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Legatura catre seara de club pentru care facem planificarea */
    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    /** Departamentul unde va sluji liderul in acea seara */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    /** Liderul care a fost ales pentru acest post */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "leader_id")
    private Leader leader;

    /** Starea cererii (daca liderul a acceptat, a refuzat sau inca asteapta) */
    private String status;
}