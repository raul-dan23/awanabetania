package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // --- TRUC: Adaugam campul title dar il marcam @Transient ---
    // Asta inseamna ca Java il vede, dar NU il cauta in baza de date SQL.
    @Transient
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String type;

    @Column(name = "visible_to")
    private String visibleTo;

    private LocalDate date;

    @Column(name = "is_visible")
    private Boolean isVisible = true;

    @Column(name = "child_id")
    private Integer childId;

    // Constructorul vechi
    public Notification(String message, String type, String visibleTo, LocalDate date) {
        this.message = message;
        this.type = type;
        this.visibleTo = visibleTo;
        this.date = date;
        this.isVisible = true;
    }

    // --- METODA CARE REZOLVA TITLUL ---
    // Cand setam titlul, il lipim automat la inceputul mesajului.
    // Astfel, se salveaza in coloana 'message' din baza de date.
    public void setTitle(String title) {
        this.title = title;
        if (title != null && !title.isEmpty()) {
            if (this.message != null) {
                this.message = "📌 " + title.toUpperCase() + "\n\n" + this.message;
            } else {
                this.message = "📌 " + title.toUpperCase();
            }
        }
    }
}