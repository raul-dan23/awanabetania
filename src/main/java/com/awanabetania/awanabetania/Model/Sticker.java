package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a rank badge (sticker) in the global catalog.
 * Stickers are ordered by ID, which also represents the child's current rank level.
 * Stickers with IDs up to and including {@code ChildProgress.lastStickerId} are
 * considered unlocked (colored); higher IDs are locked (greyed out).
 * The full set of 30 stickers is seeded by {@code DataInitializer}.
 */
@Entity
@Table(name = "sticker")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sticker {

    /** Numeric rank — both the primary key and the ordering index. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Display name shown on the progress map (e.g. "Rank 1"). */
    private String name;

    /** Server-relative path to the badge image (e.g. "/stickers/1.png"). */
    @Column(name = "image_path")
    private String imagePath;
}
