package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a functional department within the club (e.g. Games, Secretariat, Media).
 * Departments have a minimum and maximum leader capacity and an optional head leader.
 * The bidirectional many-to-many relationship with {@link Leader} uses
 * {@code @JsonIgnore} on the inverse side to prevent infinite serialization loops.
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
     * The leader who heads this department.
     * {@code @JsonIgnoreProperties} breaks the serialization cycle:
     * Department → headLeader → departments → headLeader → …
     */
    @OneToOne
    @JoinColumn(name = "head_leader_id")
    @JsonIgnoreProperties({"departments", "password", "deletionCode", "phoneNumber", "notes"})
    private Leader headLeader;

    /** All leaders who are members of this department (inverse side; not serialized). */
    @ManyToMany(mappedBy = "departments")
    @JsonIgnore
    private Set<Leader> leaders = new HashSet<>();

    /**
     * Creates a new department with capacity bounds.
     *
     * @param name       department name
     * @param minLeaders minimum required leaders
     * @param maxLeaders maximum allowed leaders
     */
    public Department(String name, Integer minLeaders, Integer maxLeaders) {
        this.name = name;
        this.minLeaders = minLeaders;
        this.maxLeaders = maxLeaders;
    }
}
