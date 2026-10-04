package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for {@link Child} entities.
 */
@Repository
public interface ChildRepository extends JpaRepository<Child, Integer> {

    /** Returns all children who have been given a manual (legacy flag). */
    List<Child> findByHasManualTrue();

    /** Returns all children whose first name contains the given substring (case-sensitive). */
    List<Child> findByNameContaining(String name);

    /** Looks up a child by their unique login username. */
    Optional<Child> findByUsername(String username);

    /** Looks up a child by their first name, ignoring case (fallback for legacy accounts). */
    Optional<Child> findByNameIgnoreCase(String name);

    /** Looks up a child by the hardware UID of their NFC card. */
    Optional<Child> findByNfcUid(String nfcUid);

    /** Returns all children assigned to the given team color, ignoring case. */
    List<Child> findByCurrentTeamIgnoreCase(String team);

    /**
     * Returns all children who are not currently assigned to a team and are not suspended,
     * ordered alphabetically by first name.
     */
    @Query("SELECT c FROM Child c WHERE (c.currentTeam IS NULL OR c.currentTeam = '') AND (c.isSuspended IS NULL OR c.isSuspended = false) ORDER BY c.name")
    List<Child> findAvailableChildren();

    /**
     * Subtracts {@code amount} from the season balance only if the balance covers it, in a
     * single UPDATE: two purchases for the same child cannot both pass a "balance is
     * enough" check and overdraw it, because the database applies them one at a time.
     *
     * @return 1 if the points were deducted, 0 if the child is unknown or has too few points
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Child c SET c.seasonPoints = c.seasonPoints - :amount WHERE c.id = :id AND c.seasonPoints >= :amount")
    int deductSeasonPoints(@Param("id") Integer id, @Param("amount") int amount);

    /**
     * Starts the season counters over for every child: points, streak, attendance, lessons,
     * badges, rewards given, suspension and team. Their previous values are archived first
     * (SeasonChildResult). Sticker progress is kept.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Child c SET c.seasonPoints = 0, c.dailyPoints = 0, c.attendanceStreak = 0, "
            + "c.totalAttendance = 0, c.lessonsCompleted = 0, c.lastAttendanceDate = NULL, c.badgesCount = 0, "
            + "c.hasManual = false, c.hasShirt = false, c.hasHat = false, c.isSuspended = false, c.currentTeam = NULL")
    int resetSeasonCounters();

    long countBySeasonPointsGreaterThan(int points);

    long countByIsSuspendedTrue();

    @Query("SELECT COALESCE(SUM(c.seasonPoints), 0) FROM Child c")
    long sumSeasonPoints();
}
