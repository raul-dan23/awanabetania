package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Reprezintă un sticker (insignă) din catalogul global.
 * Aceasta este o listă statică, ordonată după ID.
 * <br>
 * Exemplu:
 * ID 1: Rank 1
 * ID 2: Rank 2
 * ...
 */
@Entity
@Table(name = "sticker")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sticker {

    /** ID-ul reprezintă și ordinea/nivelul. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Numele vizibil al sticker-ului (ex: "Rank 1") */
    private String name;

    /** Calea către imagine (ex: "/images/badges/rank1.png") */
    @Column(name = "image_path")
    private String imagePath;
}