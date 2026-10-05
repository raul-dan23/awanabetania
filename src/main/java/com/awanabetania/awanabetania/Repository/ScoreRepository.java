package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access interface for {@link Score} entities.
 */
@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {

    /** Returns all score records for a given child (unordered). */
    List<Score> findByChildId(Integer childId);

    /** Returns all scores for a child on a specific calendar date. */
    List<Score> findByChildIdAndMeeting_Date(Integer childId, LocalDate date);

    /**
     * Checks whether a child has already been scored at a given meeting.
     * Used by {@code ScoreController} to prevent duplicate entries.
     */
    Optional<Score> findByChildIdAndMeetingId(Integer childId, Integer meetingId);

    /** Returns all scores recorded at a given meeting (used when closing a meeting). */
    List<Score> findByMeetingId(Integer meetingId);

    /** Deletes all score records belonging to a specific child. */
    @Modifying
    @Query("DELETE FROM Score s WHERE s.child.id = ?1")
    void deleteByChildId(Integer childId);

    /** A child's scores in one season, newest meeting first. */
    List<Score> findByChildIdAndSeasonIdOrderByMeeting_DateDesc(Integer childId, Integer seasonId);

    long countBySeasonId(Integer seasonId);

    /** Points earned per child in a season: rows of [childId, sum(total)]. */
    @Query("SELECT s.child.id, SUM(s.total) FROM Score s WHERE s.seasonId = ?1 GROUP BY s.child.id")
    List<Object[]> earnedPerChild(Integer seasonId);

    /**
     * The earliest meeting not closed yet that already has scores, or null: a season cannot
     * end in the middle of one.
     */
    @Query("SELECT MIN(s.meeting.date) FROM Score s WHERE s.meeting.isCompleted = false")
    LocalDate firstOpenMeetingWithScores();
}
