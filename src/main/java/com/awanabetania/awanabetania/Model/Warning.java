package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Aceasta clasa reprezinta un avertisment sau o pedeapsa.
 * Aici tinem minte daca un copil a facut ceva gresit si daca este suspendat.
 */
@Entity
@Table(name = "warnings")
@Getter
@Setter
@NoArgsConstructor
public class Warning {

    /** ID unic al avertismentului */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Motivul pentru care a fost pedepsit (descrierea faptei) */
    @Column(name = "description")
    private String description;

    /** Ne spune daca este suspendat (nu are voie sa joace) sau e doar o atentionare */
    @Column(name = "suspension")
    private Boolean suspension;

    /** Cate intalniri mai are de stat pe bara pana ii trece pedeapsa */
    @Column(name = "remaining_meetings")
    private Integer remainingMeetings;

    /** Data cand s-a intamplat incidentul */
    @Column(name = "date")
    private LocalDate date;

    /**
     * Legatura catre copilul care a gresit.
     * JsonIgnore e necesar ca sa nu se blocheze aplicatia.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    @JsonIgnore
    private Child child;

    /**
     * Un camp ajutator ca sa primim ID-ul copilului direct de pe site.
     * Transient inseamna ca nu se salveaza in baza de date.
     */
    @Transient
    private Integer childId;
}