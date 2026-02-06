package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Aceasta interfata ne ajuta sa lucram cu baza de date pentru Intalniri (Seri de Club).
 * Aici cautam serile active sau istoricul.
 */
@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {

    /**
     * Gaseste intalnirile care inca nu s-au terminat (active).
     * Le pune in ordine cronologica (cea mai veche prima).
     */
    List<Meeting> findByIsCompletedFalseOrderByDateAsc();

    /**
     * Lista cu tot istoricul serilor de club (si cele terminate, si cele active).
     * Le pune pe cele mai recente primele (ca sa vezi ultima seara sus).
     */
    List<Meeting> findAllByOrderByDateDesc();
}