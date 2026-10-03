package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * Represents a child member of the Awana club.
 * Stores personal details, attendance statistics, season/daily points,
 * inventory flags (manual, shirt, hat), NFC card UID, and links to
 * the child's progress record and manual history.
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

    /** Number of consecutive meetings attended without missing one. */
    @Column(name = "attendance_streak")
    private Integer attendanceStreak = 0;

    /** Total number of meetings attended across all seasons. */
    @Column(name = "total_attendance")
    private Integer totalAttendance = 0;

    /** Total number of lessons completed (lesson checkbox checked during scoring). */
    @Column(name = "lessons_completed")
    private Integer lessonsCompleted = 0;

    /** Date of the last meeting the child attended, set to the meeting's date (not today). */
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

    /** Cumulative points earned during the current season; used as the fair-market currency. */
    @Column(name = "season_points")
    private Integer seasonPoints = 0;

    /** Points earned during the current meeting day; reset to 0 when the meeting is closed. */
    @Column(name = "daily_points")
    private Integer dailyPoints = 0;

    /** Color name of the team the child is currently assigned to (e.g. "red"), or {@code null} if on the bench. */
    @Column(name = "current_team")
    private String currentTeam;

    // Accepted on input (login/registration) but never serialised back to the client.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private Integer progressPercent = 0;

    @Column(name = "is_suspended")
    private Boolean isSuspended = false;

    /** Short deletion-confirmation code generated on demand; must be supplied to the delete endpoint. */
    // Never serialised: knowing the code is what authorises the deletion.
    @JsonIgnore
    @Column(name = "deletion_code")
    private String deletionCode;

    /**
     * Set when a director resets the password: the temporary password works, but the app
     * asks for a new one right after login. Cleared when the account owner sets a password.
     */
    @JsonIgnore
    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    /** Hardware UID of the associated NFC card (unique per card, set by the admin). */
    @Column(name = "nfc_uid", unique = true)
    private String nfcUid;

    /** Calculated age in years; not persisted — derived from {@code birthDate} at read time. */
    @Transient
    private Integer age;

    /**
     * Calculates and returns the child's current age in full years.
     * Returns 0 if the birth date is not set.
     *
     * @return age in years, or 0 if {@code birthDate} is {@code null}
     */
    public Integer getAge() {
        if (this.birthDate == null) return 0;
        return Period.between(this.birthDate, LocalDate.now()).getYears();
    }

    /**
     * Returns whether the child currently has an active manual.
     * Checks the {@code manuals} collection first; falls back to the legacy {@code hasManual} flag.
     *
     * @return {@code true} if an ACTIVE manual exists in the collection, or if the legacy flag is set
     */
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
