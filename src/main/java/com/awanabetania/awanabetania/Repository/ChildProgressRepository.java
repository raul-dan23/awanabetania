package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.ChildProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Data access interface for {@link ChildProgress} entities.
 * The progress record is typically accessed via the {@code Child.progress} relationship,
 * but this repository is used for bulk cleanup when a child account is deleted.
 */
@Repository
public interface ChildProgressRepository extends JpaRepository<ChildProgress, Integer> {

    /** Deletes the progress record belonging to a specific child. */
    @Modifying
    @Query("DELETE FROM ChildProgress cp WHERE cp.child.id = ?1")
    void deleteByChildId(Integer childId);
}
