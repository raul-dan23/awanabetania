package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.MeetingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Aceasta interfata ne ajuta sa lucram cu baza de date pentru Planificari (Asignari).
 * Aici salvam cine la ce departament lucreaza intr-o anumita seara.
 */
@Repository
public interface MeetingAssignmentRepository extends JpaRepository<MeetingAssignment, Integer> {

    /** Gaseste toate sarcinile impartite pentru o anumita seara de club */
    List<MeetingAssignment> findByMeetingId(Integer meetingId);

    /**
     * Sterge un lider de la un departament intr-o anumita seara.
     * Spring stie sa faca stergerea automat doar citind numele lung al metodei.
     */
    void deleteByMeetingIdAndDepartmentIdAndLeaderId(Integer meetingId, Integer departmentId, Integer leaderId);
}