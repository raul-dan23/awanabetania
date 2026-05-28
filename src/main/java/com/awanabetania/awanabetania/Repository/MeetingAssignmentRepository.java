package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.MeetingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access interface for {@link MeetingAssignment} entities.
 */
@Repository
public interface MeetingAssignmentRepository extends JpaRepository<MeetingAssignment, Integer> {

    /** Returns all department assignments for a given meeting. */
    List<MeetingAssignment> findByMeetingId(Integer meetingId);

    /**
     * Removes a specific leader from a specific department slot for a given meeting.
     * Used when a director removes a leader from the schedule.
     */
    void deleteByMeetingIdAndDepartmentIdAndLeaderId(Integer meetingId, Integer departmentId, Integer leaderId);

    /** Removes all assignments for the given leader (used before deleting the leader account). */
    void deleteByLeaderId(Integer leaderId);
}
