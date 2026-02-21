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

    @Column(unique = true)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String role;

    /**
     * Departamentele asignate.
     * Am păstrat EAGER pentru login, dar am asigurat integritatea relației.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "leaders_departments",
            joinColumns = @JoinColumn(name = "leader_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<Department> departments = new HashSet<>();

    private Float rating = 0.0f;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String password;

    @Column(name = "deletion_code")
    private String deletionCode;

    public Leader(String name, String surname, String role, String password, String phoneNumber) {
        this.name = name;
        this.surname = surname;
        this.role = role;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }
}