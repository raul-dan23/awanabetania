package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.OlimpiadaScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OlimpiadaScoreRepository extends JpaRepository<OlimpiadaScore, Integer> {
    List<OlimpiadaScore> findBySessionId(Integer sessionId);

    @Transactional
    void deleteBySessionIdAndRoundNumberAndArbiterName(Integer sessionId, Integer roundNumber, String arbiterName);

    boolean existsBySessionIdAndRoundNumberAndArbiterName(Integer sessionId, Integer roundNumber, String arbiterName);
}
