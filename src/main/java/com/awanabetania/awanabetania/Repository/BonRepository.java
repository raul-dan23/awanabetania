package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Bon;
import com.awanabetania.awanabetania.Model.BonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access interface for {@link Bon} entities.
 */
@Repository
public interface BonRepository extends JpaRepository<Bon, Integer> {

    /** Returns all receipts with the given status, ordered by creation time descending. */
    List<Bon> findByStatusOrderByCreatedAtDesc(BonStatus status);

    /** Returns all receipts regardless of status, ordered by creation time descending. */
    List<Bon> findAllByOrderByCreatedAtDesc();

    /*
     * The two transitions below change the receipt only while it is still PENDING, in a
     * single UPDATE. When two cashiers approve the same receipt at once, the database runs
     * the updates one after the other: the first changes one row, the second finds the
     * receipt no longer pending and changes none. The caller checks the returned count.
     */

    /** PENDING → APPROVED. @return 1 if this call approved the receipt, 0 if it was not pending */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Bon b SET b.status = com.awanabetania.awanabetania.Model.BonStatus.APPROVED, b.approvedAt = :at "
         + "WHERE b.id = :id AND b.status = com.awanabetania.awanabetania.Model.BonStatus.PENDING")
    int markApproved(@Param("id") Integer id, @Param("at") LocalDateTime at);

    /** PENDING → REJECTED. @return 1 if this call rejected the receipt, 0 if it was not pending */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Bon b SET b.status = com.awanabetania.awanabetania.Model.BonStatus.REJECTED "
         + "WHERE b.id = :id AND b.status = com.awanabetania.awanabetania.Model.BonStatus.PENDING")
    int markRejected(@Param("id") Integer id);
}
