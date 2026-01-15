package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.TeamGamePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Aceasta interfata ne ajuta sa lucram cu baza de date pentru Punctele de Joc.
 * Varianta ACTUALIZATA: Cauta punctele in functie de Intalnire (Meeting), nu de data calendaristica.
 */
@Repository
public interface TeamGamePointRepository extends JpaRepository<TeamGamePoint, Integer> {

    /**
     * Aceasta este metoda pe care o cauta TeamController.
     * * Cum functioneaza numele metodei:
     * 1. findBy... -> Cauta
     * 2. MeetingId -> ...in campul 'meeting' dupa ID-ul lui
     * 3. And... -> ...SI
     * 4. TeamColor -> ...in campul 'teamColor' (chiar daca in DB e 'team_name', aici folosim numele variabilei din Java)
     */
    List<TeamGamePoint> findByMeetingIdAndTeamColor(Integer meetingId, String teamColor);
}