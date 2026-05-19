package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Bon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BonRepository extends JpaRepository<Bon, Integer> {
    List<Bon> findByStatusOrderByCreatedAtDesc(String status);
    List<Bon> findAllByOrderByCreatedAtDesc();
}
