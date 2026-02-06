package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Sticker;
import com.awanabetania.awanabetania.Repository.StickerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stickers")
@CrossOrigin(origins = "*")
public class StickerController {

    @Autowired
    private StickerRepository stickerRepository;

    @GetMapping
    public List<Sticker> getAllStickers() {
        // Le returnam ordonate dupa ID ca sa apara in ordinea corecta pe harta
        return stickerRepository.findAll();
    }
}