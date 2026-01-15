package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Aceasta clasa reprezinta un departament (ex: Secretari, Jocuri, Timp Biblic).
 * Aici setam regulile despre cati oameni sunt necesari.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    /** ID unic al departamentului */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Numele departamentului */
    @Column(nullable = false)
    private String name;

    /** Numarul minim de lideri de care avem nevoie aici */
    @Column(name = "min_leaders")
    private Integer minLeaders;

    /** Numarul maxim de lideri acceptati aici */
    @Column(name = "max_leaders")
    private Integer maxLeaders;

    /** Cine este seful acestui departament */
    @OneToOne
    @JoinColumn(name = "head_leader_id")
    private Leader headLeader;

    /**
     * Lista cu liderii care fac parte din acest departament.
     * JsonIgnore opreste o eroare tehnica (bucla infinita).
     */
    @ManyToMany(mappedBy = "departments")
    @JsonIgnore
    private Set<Leader> leaders = new HashSet<>();

    /** Constructor pentru a face un departament nou mai usor */
    public Department(String name, Integer minLeaders, Integer maxLeaders) {
        this.name = name;
        this.minLeaders = minLeaders;
        this.maxLeaders = maxLeaders;
    }
}