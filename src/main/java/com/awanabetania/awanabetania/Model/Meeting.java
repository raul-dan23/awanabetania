package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Represents a single club evening (meeting).
 * Tracks the date, whether the meeting has been closed, an optional director-of-the-day,
 * a general rating/feedback entered at the end of the evening, and a PIN that is
 * auto-generated when a secretariat leader is assigned — used to gate the scoring screen.
 */
@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The season this row belongs to (see {@link Season}); set by the server, never by the client. */
    @JsonIgnore
    @Column(name = "season_id")
    private Integer seasonId;

    private LocalDate date;

    private String description;

    /** {@code false} while the meeting is in progress; {@code true} after it has been closed. */
    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "general_rating")
    private Integer generalRating;

    @Column(name = "general_feedback", columnDefinition = "TEXT")
    private String generalFeedback;

    /** The leader responsible for running this particular evening. */
    @ManyToOne
    @JoinColumn(name = "director_day_id")
    private Leader directorDay;

    /**
     * 4-digit PIN generated with {@link java.security.SecureRandom} when the first secretariat
     * assignment is created. Not regenerated on subsequent secretariat assignments for the same meeting.
     */
    @Column(name = "meeting_pin")
    private String meetingPin;
}
