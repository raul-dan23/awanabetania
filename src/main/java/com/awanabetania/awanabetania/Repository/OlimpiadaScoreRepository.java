package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.OlimpiadaScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Data access interface for {@link OlimpiadaScore} entities.
 */
@Repository
public interface OlimpiadaScoreRepository extends JpaRepository<OlimpiadaScore, Integer> {

    /** Returns all score entries for the given session. */
    List<OlimpiadaScore> findBySessionId(Integer sessionId);

    /**
     * Removes all score entries for a given arbiter's submission of a specific round.
     * Called before re-saving a round to allow the arbiter to correct their input.
     */
    @Transactional
    void deleteBySessionIdAndRoundNumberAndArbiterName(Integer sessionId, Integer roundNumber, String arbiterName);

    /**
     * Returns {@code true} if the given arbiter has already submitted scores for the given round.
     * Used to show a warning in the UI before overwriting.
     */
    boolean existsBySessionIdAndRoundNumberAndArbiterName(Integer sessionId, Integer roundNumber, String arbiterName);
}
