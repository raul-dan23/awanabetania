package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.TeamGamePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access interface for {@link TeamGamePoint} entities.
 */
@Repository
public interface TeamGamePointRepository extends JpaRepository<TeamGamePoint, Integer> {

    /**
     * Returns all game-point entries for a given team during a specific meeting.
     * The sum of the returned entries' {@code points} fields is the team's game score for that evening.
     *
     * @param meetingId  the meeting to query
     * @param teamColor  the team color identifier (e.g. "red")
     */
    List<TeamGamePoint> findByMeetingIdAndTeamColor(Integer meetingId, String teamColor);
}
