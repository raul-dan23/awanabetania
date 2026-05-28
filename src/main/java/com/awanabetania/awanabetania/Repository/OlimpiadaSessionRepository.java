package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.OlimpiadaSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for {@link OlimpiadaSession} entities.
 */
@Repository
public interface OlimpiadaSessionRepository extends JpaRepository<OlimpiadaSession, Integer> {

    /** Looks up a session by its unique access code. */
    Optional<OlimpiadaSession> findByCode(String code);

    /** Returns all sessions ordered by creation time descending (most recent first). */
    List<OlimpiadaSession> findAllByOrderByCreatedAtDesc();
}
