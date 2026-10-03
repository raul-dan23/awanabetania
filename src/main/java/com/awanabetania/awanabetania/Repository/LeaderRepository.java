package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Leader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for {@link Leader} entities.
 */
@Repository
public interface LeaderRepository extends JpaRepository<Leader, Integer> {

    /** Looks up a leader by exact first and last name (used for duplicate checking during registration). */
    Optional<Leader> findByNameAndSurname(String name, String surname);

    /** Returns all leaders who are members of the given department. */
    List<Leader> findByDepartmentsId(Integer departmentId);

    /** Looks up a leader by phone number. */
    Optional<Leader> findByPhoneNumber(String phoneNumber);

    /** Looks up a leader by exact first name. */
    Optional<Leader> findByName(String name);

    /** Looks up a leader by first name, ignoring case (fallback for legacy login by name). */
    Optional<Leader> findByNameIgnoreCase(String name);

    /** Looks up a leader by their unique login username. */
    Optional<Leader> findByUsername(String username);

    /** Returns all leaders whose role matches any of the given values (case-insensitive). */
    List<Leader> findByRoleIgnoreCaseIn(List<String> roles);

    /** The leader bound to a Google account (its permanent id). */
    Optional<Leader> findByGoogleSub(String googleSub);

    /** The leader invited with this Google address. */
    Optional<Leader> findByEmailIgnoreCase(String email);
}
