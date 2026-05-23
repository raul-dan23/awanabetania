package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "olimpiada_scores")
@Getter @Setter @NoArgsConstructor
public class OlimpiadaScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id")
    private Integer sessionId;

    @Column(name = "round_number")
    private Integer roundNumber;

    private String team;

    @Column(name = "arbiter_name")
    private String arbiterName;

    private Integer place;

    private Integer points;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
