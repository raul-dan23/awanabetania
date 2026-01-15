package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Aceasta clasa reprezinta o seara de club sau o intalnire.
 * Aici tinem minte data, cine a fost director si cum a decurs totul.
 */
@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
public class Meeting {

    /** ID unic al intalnirii */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Data cand are loc intalnirea */
    private LocalDate date;

    /** O scurta descriere a ce facem in acea seara */
    private String description;

    /** Daca seara s-a terminat sau inca e in desfasurare */
    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    /** O nota generala pentru cum a mers toata seara */
    @Column(name = "general_rating")
    private Integer generalRating;

    /** Pareri scrise despre cum a fost atmosfera */
    @Column(name = "general_feedback", columnDefinition = "TEXT")
    private String generalFeedback;

    /** Cine este liderul responsabil (directorul de zi) pentru aceasta data */
    @ManyToOne
    @JoinColumn(name = "director_day_id")
    private Leader directorDay;
}