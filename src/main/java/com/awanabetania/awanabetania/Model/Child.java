package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Clasa principală pentru un copil din club.
 * Conține date personale, statistici și legătura cu progresul (stickerele).
 */
@Entity
@Table(name = "children")
@Getter
@Setter
@NoArgsConstructor
public class Child {

    @Column(unique = true)
    private String username;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String surname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "attendance_streak")
    private Integer attendanceStreak = 0;

    @Column(name = "total_attendance")
    private Integer totalAttendance = 0;

    @Column(name = "lessons_completed")
    private Integer lessonsCompleted = 0;

    @Column(name = "last_attendance_date")
    private LocalDate lastAttendanceDate;

    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ChildManual> manuals = new ArrayList<>();

    @OneToOne(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private ChildProgress progress;

    @Column(name = "has_manual")
    private Boolean hasManual = false;

    @Column(name = "has_shirt")
    private Boolean hasShirt = false;

    @Column(name = "has_hat")
    private Boolean hasHat = false;

    @Column(name = "badges_count")
    private Integer badgesCount = 0;

    @Column(name = "season_points")
    private Integer seasonPoints = 0;

    @Column(name = "daily_points")
    private Integer dailyPoints = 0;

    @Column(name = "current_team")
    private String currentTeam;

    private String password;

    private Integer progressPercent = 0;

    @Column(name = "is_suspended")
    private Boolean isSuspended = false;

    @Column(name = "deletion_code")
    private String deletionCode;

    @Transient
    private Integer age;

    public Integer getAge() {
        if (this.birthDate == null) return 0;
        return Period.between(this.birthDate, LocalDate.now()).getYears();
    }

    @JsonProperty("hasManual")
    public Boolean getHasManual() {
        if (manuals != null && !manuals.isEmpty()) {
            boolean hasActiveManual = manuals.stream()
                    .anyMatch(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()));
            if (hasActiveManual) return true;
        }
        return this.hasManual;
    }
}