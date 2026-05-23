package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "olimpiada_sessions")
@Getter @Setter @NoArgsConstructor
public class OlimpiadaSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(unique = true, length = 10)
    private String code;

    private String status = "ACTIVE";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
