package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Aceasta clasa reprezinta un lider sau voluntar din aplicatie.
 * Aici stocam datele de contact, parola si departamentele unde e implicat.
 */
@Entity
@Table(name = "leaders")
@Getter
@Setter
@NoArgsConstructor
public class Leader {

    /** ID unic generat automat */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Numele de familie */
    @Column(nullable = false)
    private String name;

    /** Prenumele liderului */
    @Column(nullable = false)
    private String surname;

    /** Numarul de telefon */
    @Column(name = "phone_number")
    private String phoneNumber;

    /** Rolul in organizatie (Lider, Director sau Coordonator) */
    private String role;

    /**
     * Lista cu departamentele unde activeaza acest lider.
     * EAGER inseamna ca le incarcam imediat cand citim liderul din baza de date.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "leaders_departments",
            joinColumns = @JoinColumn(name = "leader_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<Department> departments = new HashSet<>();

    /** Nota medie primita de la directori */
    private Float rating = 0.0f;

    /** Alte observatii sau notite despre el */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Parola folosita la logare */
    private String password;

    /** Codul solicitat sa poata sterge contul */
    @Column(name = "deletion_code")
    private String deletionCode;

    /** Constructor pentru a crea un lider nou mai rapid */
    public Leader(String name, String surname, String role, String password, String phoneNumber) {
        this.name = name;
        this.surname = surname;
        this.role = role;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }
}