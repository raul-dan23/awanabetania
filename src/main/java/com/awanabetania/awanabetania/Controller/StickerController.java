package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Sticker;
import com.awanabetania.awanabetania.Repository.StickerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Provides read-only access to the sticker (rank badge) catalog.
 * The frontend uses the ordered list to render the progress map,
 * coloring stickers up to the child's {@code lastStickerId} and greying the rest.
 */
@RestController
@RequestMapping("/api/stickers")
public class StickerController {

    @Autowired
    private StickerRepository stickerRepository;

    /**
     * Returns all stickers ordered by ID (ascending), which corresponds to ascending rank.
     *
     * @return list of all {@link Sticker} entities
     */
    @GetMapping
    public List<Sticker> getAllStickers() {
        return stickerRepository.findAll();
    }
}
