package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Entitatea Leader (Lider).
 * Reprezintă un utilizator cu drepturi de administrare sau voluntar.
 */
@Entity
@Table(name = "leaders")
@Getter
@Setter
@NoArgsConstructor
public class Leader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * Roluri posibile: "Lider", "Director", "Coordonator", "Secretar".
     * Folosit pentru a determina permisiunile în frontend.
     */
    private String role;

    /**
     * Departamentele asignate (ex: Jocuri, Manual, Secretariat).
     * EAGER = Se încarcă imediat (necesar la login pentru a ști unde are acces).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "leaders_departments",
            joinColumns = @JoinColumn(name = "leader_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<Department> departments = new HashSet<>();

    /** Nota medie (dacă se folosește sistemul de evaluare). */
    private Float rating = 0.0f;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Parola stocată (în viitor o vom cripta). */
    private String password;

    /**
     * Codul opțional de securitate pentru ștergerea contului.
     * Dacă este setat, liderul nu poate fi șters fără acest cod.
     */
    @Column(name = "deletion_code")
    private String deletionCode;

    // --- Constructor ajutător pentru înregistrare ---
    public Leader(String name, String surname, String role, String password, String phoneNumber) {
        this.name = name;
        this.surname = surname;
        this.role = role;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }
}