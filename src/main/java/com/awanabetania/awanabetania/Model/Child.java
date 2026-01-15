package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

/**
 * Clasa principala pentru un copil din club.
 * Aici tinem toate datele lui personale, punctele si progresul.
 * * UPDATE: Aceasta versiune suporta logica simplificata pentru echipe si puncte zilnice.
 */
@Entity
@Table(name = "children")
@Getter
@Setter
@NoArgsConstructor
public class Child {

    /** ID unic generat automat de baza de date */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Numele de familie */
    private String name;

    /** Prenumele copilului */
    private String surname;

    /** Data nasterii, o folosim ca sa aflam varsta */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** Numele parintelui */
    @Column(name = "parent_name")
    private String parentName;

    /** Telefonul parintelui */
    @Column(name = "parent_phone")
    private String parentPhone;

    // --- STATISTICI SI PREZENTA ---

    /** Cate dati a venit la rand fara pauza (streak) */
    @Column(name = "attendance_streak")
    private Integer attendanceStreak = 0;

    /** De cate ori a venit la club in total */
    @Column(name = "total_attendance")
    private Integer totalAttendance = 0;

    /** Numarul de lectii terminate */
    @Column(name = "lessons_completed")
    private Integer lessonsCompleted = 0;

    // --- INVENTAR (Ce are la el) ---

    /** Daca are manualul cumparat */
    @Column(name = "has_manual")
    private Boolean hasManual = false;

    /** Daca are tricoul (uniforma) */
    @Column(name = "has_shirt")
    private Boolean hasShirt = false;

    /** Daca are palarie (optional) */
    @Column(name = "has_hat")
    private Boolean hasHat = false;

    /** Numarul de insigne castigate */
    @Column(name = "badges_count")
    private Integer badgesCount = 0;

    // --- SISTEMUL DE PUNCTE (SIMPLIFICAT) ---

    /** * Punctele stranse in acest an (SEZON).
     * Acestea sunt BANII copilului si NU se sterg la finalul serii.
     */
    @Column(name = "season_points")
    private Integer seasonPoints = 0;

    /** * Punctele facute strict AZI.
     * Acestea se aduna la scorul echipei in timp real.
     * ATENTIE: Se reseteaza la 0 cand directorul apasa "Incheie Seara".
     */
    @Column(name = "daily_points")
    private Integer dailyPoints = 0;

    /** * Echipa in care este repartizat AZI (ex: "red", "blue").
     * ATENTIE: Se sterge (devine null) cand directorul apasa "Incheie Seara".
     */
    @Column(name = "current_team")
    private String currentTeam;

    // --- ALTE DETALII ---

    /** Parola pentru cand se logheaza copilul in aplicatie */
    private String password;

    /** Progresul general (bara de progres vizuala) */
    private Integer progress = 0;

    /** Daca este suspendat nu poate intra la joc si nu apare in liste */
    @Column(name = "is_suspended")
    private Boolean isSuspended = false;

    /** Codul solicitat sa poata sterge contul (GDPR) */
    @Column(name = "deletion_code")
    private String deletionCode;

    /** Varsta calculata pe loc, nu se salveaza in baza de date */
    @Transient
    private Integer age;

    /** Calculeaza varsta pe baza datei de nastere */
    public Integer getAge() {
        if (this.birthDate == null) return 0;
        return Period.between(this.birthDate, LocalDate.now()).getYears();
    }
}