package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a scoring session for the annual Awana Olympiad competition.
 * Each session has a unique short code (up to 10 characters, e.g. "OLM26") that
 * arbiters use to access the scoring screen without a full account.
 * A session starts as ACTIVE and is closed by the director when judging ends.
 */
@Entity
@Table(name = "olimpiada_sessions")
@Getter @Setter @NoArgsConstructor
public class OlimpiadaSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Descriptive name of this session (e.g. "Olympiad 2026"). */
    private String name;

    /** Short unique access code shared with arbiters (case-insensitive, stored upper-case). */
    @Column(unique = true, length = 10)
    private String code;

    /** Session state: "ACTIVE" while scoring is open, "CLOSED" when the director ends it. */
    private String status = "ACTIVE";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
