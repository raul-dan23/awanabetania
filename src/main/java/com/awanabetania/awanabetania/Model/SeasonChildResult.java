package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What a child had when a season closed: the counters kept on {@link Child}, which the new
 * season sets back to zero. Points earned and warnings are not copied here; they are
 * counted from the season's scores and warnings, which stay in the database.
 */
@Entity
@Table(name = "season_child_results")
@Getter @Setter @NoArgsConstructor
public class SeasonChildResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "season_id", nullable = false)
    private Integer seasonId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    /** Point balance at the end of the season, after the fair. */
    @Column(name = "season_points", nullable = false)
    private int seasonPoints;

    @Column(name = "total_attendance", nullable = false)
    private int totalAttendance;

    /** Consecutive attendances at the end of the season. */
    @Column(name = "attendance_streak", nullable = false)
    private int attendanceStreak;

    @Column(name = "lessons_completed", nullable = false)
    private int lessonsCompleted;

    @Column(name = "badges_count", nullable = false)
    private int badgesCount;

    @Column(name = "had_manual", nullable = false)
    private boolean hadManual;

    @Column(name = "had_shirt", nullable = false)
    private boolean hadShirt;

    @Column(name = "had_hat", nullable = false)
    private boolean hadHat;

    /** The child's handbooks that season, as JSON: [{name, status, startDate, endDate}]. */
    @Column(columnDefinition = "TEXT")
    private String manuals;
}
