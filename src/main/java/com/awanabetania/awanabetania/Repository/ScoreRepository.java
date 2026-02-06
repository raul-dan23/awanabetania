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
 * Aceasta interfata ne ajuta sa lucram cu baza de date pentru Punctaje (Scoruri).
 * Aici salvam si citim punctele pe care le primesc copiii in fiecare seara.
 */
@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {

    /** Gaseste toate fisele de punctaj ale unui singur copil */
    List<Score> findByChildId(Integer childId);

    /**
     * Gaseste tot istoricul punctelor unui copil.
     * Le aranjeaza cronologic, de la cea mai recenta intalnire la cea mai veche.
     * Folosit in pagina de Profil a copilului.
     */
    List<Score> findByChildIdOrderByMeeting_DateDesc(Integer childId);

    /** * Cauta punctele primite de un copil intr-o anumita zi (data calendaristica).
     * Folosit pentru verificari rapide pe data curenta.
     */
    List<Score> findByChildIdAndMeeting_Date(Integer childId, LocalDate date);

    /**
     * Verifica daca un copil a fost deja punctat la o anumita intalnire (dupa ID).
     * Aceasta metoda este CRITICA pentru TeamController, ca sa adune punctele la echipa.
     */
    Optional<Score> findByChildIdAndMeetingId(Integer childId, Integer meetingId);

    // FIX: Stergere
    @Modifying
    @Query("DELETE FROM Score s WHERE s.child.id = ?1")
    void deleteByChildId(Integer childId);

}

