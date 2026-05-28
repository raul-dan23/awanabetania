package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Warning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access interface for {@link Warning} entities.
 */
@Repository
public interface WarningRepository extends JpaRepository<Warning, Integer> {

    /** Returns all warnings for a given child, ordered newest first. */
    @Query("SELECT w FROM Warning w WHERE w.child.id = :childId ORDER BY w.id DESC")
    List<Warning> findByChildIdOrderByIdDesc(@Param("childId") Integer childId);

    /**
     * Returns all active suspensions (suspension=true) where the countdown has not yet
     * reached zero. Used during meeting close to decrement remaining meetings.
     *
     * @param count threshold — only records with {@code remainingMeetings > count} are returned
     */
    List<Warning> findBySuspensionTrueAndRemainingMeetingsGreaterThan(int count);

    /** Deletes all warnings belonging to a specific child. */
    @Modifying
    @Query("DELETE FROM Warning w WHERE w.child.id = ?1")
    void deleteByChildId(Integer childId);
}
