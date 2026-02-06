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

    @Column(columnDefinition = "TEXT")
    private String message;

    private String type;

    @Column(name = "visible_to")
    private String visibleTo;

    private LocalDate date;

    @Column(name = "is_visible")
    private Boolean isVisible = true;

    // --- CÂMP NOU PENTRU LEGĂTURA CU COPILUL ---
    @Column(name = "child_id")
    private Integer childId; // Nullable, doar pentru notificari legate de premii

    // Constructor vechi (pentru compatibilitate)
    public Notification(String message, String type, String visibleTo, LocalDate date) {
        this.message = message;
        this.type = type;
        this.visibleTo = visibleTo;
        this.date = date;
        this.isVisible = true;
    }
}