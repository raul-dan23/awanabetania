package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // <--- IMPORT NOU
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Aceasta clasa reprezinta un departament (ex: Secretari, Jocuri).
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "min_leaders")
    private Integer minLeaders;

    @Column(name = "max_leaders")
    private Integer maxLeaders;

    /**
     * Cine este seful acestui departament.
     * @JsonIgnoreProperties("departments") este CRITIC aici.
     * Ii spune serverului: "Arata-mi seful, dar nu intra in lista lui de departamente".
     * Asta rupe bucla infinita: Leader -> Department -> HeadLeader -> Departments...
     */
    @OneToOne
    @JoinColumn(name = "head_leader_id")
    @JsonIgnoreProperties({"departments", "password", "deletionCode", "phoneNumber", "notes"})
    private Leader headLeader;

    @ManyToMany(mappedBy = "departments")
    @JsonIgnore
    private Set<Leader> leaders = new HashSet<>();

    public Department(String name, Integer minLeaders, Integer maxLeaders) {
        this.name = name;
        this.minLeaders = minLeaders;
        this.maxLeaders = maxLeaders;
    }
}