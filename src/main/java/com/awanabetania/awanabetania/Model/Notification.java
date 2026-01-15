package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Aceasta clasa reprezinta un mesaj sau o alerta pentru utilizatori.
 * Aici salvam instiintarile care apar pe ecran (Dashboard).
 * UPDATE: Suporta stergerea logica (Soft Delete) prin campul isVisible.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    /** ID unic al notificarii */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Textul propriu-zis al mesajului. Poate fi lung. */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** Tipul mesajului (de exemplu Alerta, Informatie sau Feedback). Ne ajuta sa stim ce culoare sa aiba. */
    private String type;

    /** Cine poate vedea mesajul. Poate fi ID-ul unui singur lider sau textul ALL pentru toata lumea. */
    @Column(name = "visible_to")
    private String visibleTo;

    /** Data cand a fost trimis mesajul */
    private LocalDate date;

    /**
     * Daca este TRUE, mesajul apare pe ecran.
     * Daca este FALSE, inseamna ca utilizatorul l-a sters (dar noi il pastram in baza de date).
     */
    @Column(name = "is_visible")
    private Boolean isVisible = true;

    /** Constructor special ca sa putem crea o notificare noua dintr-o singura linie de cod */
    public Notification(String message, String type, String visibleTo, LocalDate date) {
        this.message = message;
        this.type = type;
        this.visibleTo = visibleTo;
        this.date = date;
        this.isVisible = true; // Implicit e vizibil
    }
}