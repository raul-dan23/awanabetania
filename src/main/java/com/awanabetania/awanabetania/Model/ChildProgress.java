package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Tracks a child's progression through the sticker ranking system.
 * Each child has exactly one {@code ChildProgress} record (OneToOne).
 * {@code lastStickerId} represents the highest rank unlocked: stickers with IDs
 * from 1 to {@code lastStickerId} are shown as colored (unlocked);
 * those with higher IDs are shown as grey (locked).
 */
@Entity
@Table(name = "child_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** The child this progress record belongs to. */
    @OneToOne
    @JoinColumn(name = "child_id")
    @JsonIgnore
    @ToString.Exclude
    private Child child;

    /** Total number of manuals the child has started (incremented when a new manual is issued). */
    @Column(name = "manuals_count")
    private Integer manualsCount = 0;

    /**
     * ID of the highest sticker unlocked so far.
     * A value of 0 means no stickers have been unlocked yet.
     */
    @Column(name = "last_sticker_id")
    private Integer lastStickerId = 0;
}
