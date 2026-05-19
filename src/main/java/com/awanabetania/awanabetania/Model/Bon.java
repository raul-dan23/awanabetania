package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bons")
@Getter @Setter @NoArgsConstructor
public class Bon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "child_id")
    @JsonIgnoreProperties({"manuals", "progress", "password", "deletionCode", "hibernateLazyInitializer"})
    private Child child;

    @Column(name = "leader_name")
    private String leaderName;

    @Column(columnDefinition = "TEXT")
    private String items;

    @Column(name = "total_points")
    private Integer totalPoints;

    private String status = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
