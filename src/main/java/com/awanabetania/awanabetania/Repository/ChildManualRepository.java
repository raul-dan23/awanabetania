package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.ChildManual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Data access interface for {@link ChildManual} entities.
 */
@Repository
public interface ChildManualRepository extends JpaRepository<ChildManual, Integer> {

    /** Deletes all manual records belonging to a specific child. */
    @Modifying
    @Query("DELETE FROM ChildManual cm WHERE cm.child.id = ?1")
    void deleteByChildId(Integer childId);
}
