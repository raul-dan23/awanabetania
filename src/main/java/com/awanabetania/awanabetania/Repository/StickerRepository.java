package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Sticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access interface for {@link Sticker} entities.
 * Standard CRUD operations are sufficient; stickers are seeded by {@code DataInitializer}.
 */
@Repository
public interface StickerRepository extends JpaRepository<Sticker, Integer> {
}
