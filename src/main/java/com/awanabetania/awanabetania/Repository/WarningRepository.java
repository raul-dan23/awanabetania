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

    /** Deletes all warnings belonging to a specific child. */
    @Modifying
    @Query("DELETE FROM Warning w WHERE w.child.id = ?1")
    void deleteByChildId(Integer childId);

    /** A child's warnings in one season, newest first. */
    @Query("SELECT w FROM Warning w WHERE w.child.id = :childId AND w.seasonId = :seasonId ORDER BY w.id DESC")
    List<Warning> findByChildIdAndSeasonIdOrderByIdDesc(@Param("childId") Integer childId, @Param("seasonId") Integer seasonId);

    /** Suspensions still running in a season; those of a closed season ended with it. */
    List<Warning> findBySeasonIdAndSuspensionTrueAndRemainingMeetingsGreaterThan(Integer seasonId, int count);

    /** Warnings per child in a season: rows of [childId, count]. */
    @Query("SELECT w.child.id, COUNT(w) FROM Warning w WHERE w.seasonId = ?1 GROUP BY w.child.id")
    List<Object[]> countPerChild(Integer seasonId);
}
