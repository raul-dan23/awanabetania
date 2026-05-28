package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Represents an in-app notification displayed in the dashboard.
 * Notifications are soft-deleted: setting {@code isVisible} to {@code false} hides them
 * without removing them from the database.
 *
 * <p>The {@code visibleTo} field controls the audience:
 * <ul>
 *   <li>"ALL" — visible to all leaders</li>
 *   <li>"DIRECTOR" — visible only to directors and coordinators</li>
 *   <li>A numeric string (e.g. "42") — visible only to the leader with that ID</li>
 * </ul>
 *
 * <p>The optional {@code title} field is transient: when {@link #setTitle(String)} is called,
 * the title is prepended to the {@code message} column so that no schema change is needed.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Optional display title. Transient — not stored in a separate column.
     * When set, the value is prepended to {@code message} by {@link #setTitle(String)}.
     */
    @Transient
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String type;

    /** Audience selector: "ALL", "DIRECTOR", or a specific leader ID as a string. */
    @Column(name = "visible_to")
    private String visibleTo;

    private LocalDate date;

    @Column(name = "is_visible")
    private Boolean isVisible = true;

    /** ID of the child this notification is about, if applicable. */
    @Column(name = "child_id")
    private Integer childId;

    /**
     * Constructs a notification without a title.
     *
     * @param message   notification body text
     * @param type      category string (e.g. "INFO", "ALERT", "FEEDBACK")
     * @param visibleTo audience selector
     * @param date      notification date
     */
    public Notification(String message, String type, String visibleTo, LocalDate date) {
        this.message = message;
        this.type = type;
        this.visibleTo = visibleTo;
        this.date = date;
        this.isVisible = true;
    }

    /**
     * Sets the transient title and prepends it to the persisted {@code message} field.
     * This avoids requiring a dedicated {@code title} column in the database.
     *
     * @param title the title text to display above the message body
     */
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
