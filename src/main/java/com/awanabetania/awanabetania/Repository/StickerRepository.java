package com.awanabetania.awanabetania.Repository;
import com.awanabetania.awanabetania.Model.Sticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StickerRepository extends JpaRepository<Sticker, Integer> {
}