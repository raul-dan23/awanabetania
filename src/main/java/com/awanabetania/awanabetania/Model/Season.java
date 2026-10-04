package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * A club year. Scores, meetings, leader evaluations, warnings and fair receipts carry the
 * season they were recorded in, and the app shows the active one. Starting a new season
 * closes the current one, keeps its records for reference and sets the children's points,
 * streaks and rewards and the leaders' ratings back to zero (see {@code SeasonService}).
 */
@Entity
@Table(name = "seasons")
@Getter @Setter @NoArgsConstructor
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** The day the season was closed; {@code null} while it is active. */
    @Column(name = "end_date")
    private LocalDate endDate;

    // Stored as text in a VARCHAR column, like BonStatus
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private SeasonStatus status;

    public Season(String name, LocalDate startDate) {
        this.name = name;
        this.startDate = startDate;
        this.status = SeasonStatus.ACTIVE;
    }
}
