package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access interface for {@link Meeting} entities.
 */
@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {

    /**
     * Returns all meetings that have not yet been closed, ordered by date ascending.
     * The first result is the current (or next) active meeting.
     */
    List<Meeting> findByIsCompletedFalseOrderByDateAsc();

    /** Returns all meetings ordered by date descending (most recent first). */
    List<Meeting> findAllByOrderByDateDesc();

    /** Returns all meetings where the given leader is assigned as director-of-the-day. */
    List<Meeting> findByDirectorDayId(Integer directorDayId);

    long countBySeasonIdAndIsCompletedTrue(Integer seasonId);

    long countBySeasonIdAndIsCompletedFalse(Integer seasonId);

    /** Planned meetings that have not happened yet belong to the new season. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Meeting m SET m.seasonId = :to WHERE m.seasonId = :from AND m.isCompleted = false")
    int moveOpenMeetings(@Param("from") Integer from, @Param("to") Integer to);
}
