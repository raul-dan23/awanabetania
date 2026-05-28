package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.LeaderEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Data access interface for {@link LeaderEvaluation} entities.
 */
@Repository
public interface LeaderEvaluationRepository extends JpaRepository<LeaderEvaluation, Integer> {

    /** Returns all visible evaluations recorded on a given date. */
    List<LeaderEvaluation> findByDateAndIsVisibleTrue(LocalDate date);

    /** Returns all visible evaluations for a given leader, newest first. */
    List<LeaderEvaluation> findByLeaderIdAndIsVisibleTrueOrderByDateDesc(Integer leaderId);

    /** Deletes all evaluations associated with a given leader (used before deleting the leader). */
    void deleteByLeaderId(Integer leaderId);

    /** Deletes all evaluations submitted by a given evaluator (used before deleting the evaluator). */
    void deleteByEvaluatedBy(Integer evaluatedBy);
}
