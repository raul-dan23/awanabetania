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

    /** Returns all score records for a given child, newest meeting first. */
    List<Score> findByChildIdOrderByMeeting_DateDesc(Integer childId);

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
}
