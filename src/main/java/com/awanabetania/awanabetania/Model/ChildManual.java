package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Reprezintă istoricul unui manual primit de un copil.
 * <br>
 * Rol: Informativ și statistic. Ne ajută să știm ce cărți a parcurs copilul.
 * NU conține progresul lecțiilor (acela e în ChildProgress).
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

    /** Copilul care a primit manualul */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    @JsonIgnore
    @ToString.Exclude
    private Child child;

    /** Numele manualului (ex: "Manual 1") */
    private String name;

    /** Status: ACTIVE (în lucru) sau COMPLETED (terminat) */
    private String status;

    /** Data primirii */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Data finalizării */
    @Column(name = "end_date")
    private LocalDate endDate;
}