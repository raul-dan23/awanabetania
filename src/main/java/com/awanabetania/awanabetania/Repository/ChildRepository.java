package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
