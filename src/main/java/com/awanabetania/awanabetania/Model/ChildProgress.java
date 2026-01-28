package com.awanabetania.awanabetania.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * "Save Game"-ul copilului.
 * Ține minte nivelul curent (ultimul sticker deblocat) și statistici globale.
 * <br>
 * Avem un singur obiect de acest tip pentru fiecare copil (Relație 1-la-1).
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

    /** Copilul de care aparține acest progres */
    @OneToOne
    @JoinColumn(name = "child_id")
    @JsonIgnore
    @ToString.Exclude
    private Child child;

    /** * Numărul total de manuale începute.
     * Crește automat când îi dai un manual nou.
     */
    @Column(name = "manuals_count")
    private Integer manualsCount = 0;

    /** * ID-ul ultimului sticker deblocat.
     * <br>
     * Exemplu: Dacă este 5, înseamnă că stickerele cu ID 1, 2, 3, 4, 5 sunt deblocate (Colorate).
     * Cele cu ID > 5 sunt blocate (Gri).
     */
    @Column(name = "last_sticker_id")
    private Integer lastStickerId = 0;
}