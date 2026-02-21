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

    @Column(unique = true) // Ne asiguram ca nu exista 2 username-uri identice
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

    // --- STATISTICI ---
    @Column(name = "attendance_streak")
    private Integer attendanceStreak = 0;

    @Column(name = "total_attendance")
    private Integer totalAttendance = 0;

    @Column(name = "lessons_completed")
    private Integer lessonsCompleted = 0;

    @Column(name = "last_attendance_date")
    private LocalDate lastAttendanceDate;

    // --- MANUALE ȘI PROGRES (SISTEM NOU) ---

    /**
     * ISTORIC MANUALE: Lista informativă a cărților primite (ex: HangGlider, WingRunner).
     * Folosit doar pentru a vedea ce cărți a avut în trecut.
     */
    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ChildManual> manuals = new ArrayList<>();

    /**
     * CURSOR GLOBAL ("Save Game"):
     * Aici ținem minte ID-ul ultimului sticker deblocat și câte manuale a avut în total.
     * Relație 1-la-1: Un copil are un singur rând de progres activ.
     */
    @OneToOne(mappedBy = "child", cascade = CascadeType.ALL)
    @ToString.Exclude
    private ChildProgress progress;

    // --- SISTEM VECHI (Legacy) ---
    // Păstrat pentru compatibilitate, dar calculat dinamic mai jos.
    @Column(name = "has_manual")
    private Boolean hasManual = false;

    @Column(name = "has_shirt")
    private Boolean hasShirt = false;

    @Column(name = "has_hat")
    private Boolean hasHat = false;

    @Column(name = "badges_count")
    private Integer badgesCount = 0;

    // --- PUNCTE ---
    @Column(name = "season_points")
    private Integer seasonPoints = 0;

    @Column(name = "daily_points")
    private Integer dailyPoints = 0;

    @Column(name = "current_team")
    private String currentTeam;

    // --- ALTE DETALII ---
    private String password;

    // Progresul vizual general (bara de progres), poate fi calculat sau setat manual
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

    /**
     * LOGICA HIBRIDĂ:
     * Verificăm dacă are vreun manual activ în istoric pentru a răspunde cu TRUE la 'hasManual'.
     * Astfel, frontend-ul vechi va funcționa fără modificări.
     */
    @JsonProperty("hasManual")
    public Boolean getHasManual() {
        if (manuals != null && !manuals.isEmpty()) {
            // Dacă are un manual marcat ca ACTIVE în istoric, considerăm că are manual
            boolean hasActiveManual = manuals.stream()
                    .anyMatch(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()));
            if (hasActiveManual) return true;
        }
        return this.hasManual;
    }
}