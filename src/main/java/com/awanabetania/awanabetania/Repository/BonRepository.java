package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Bon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access interface for {@link Bon} entities.
 */
@Repository
public interface BonRepository extends JpaRepository<Bon, Integer> {

    /** Returns all receipts with the given status, ordered by creation time descending. */
    List<Bon> findByStatusOrderByCreatedAtDesc(String status);

    /** Returns all receipts regardless of status, ordered by creation time descending. */
    List<Bon> findAllByOrderByCreatedAtDesc();
}
