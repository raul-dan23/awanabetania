package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
