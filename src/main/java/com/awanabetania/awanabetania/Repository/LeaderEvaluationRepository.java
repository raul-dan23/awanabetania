package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.LeaderEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /** Deletes all evaluations associated with a given leader (used before deleting the leader). */
    void deleteByLeaderId(Integer leaderId);

    /** Deletes all evaluations submitted by a given evaluator (used before deleting the evaluator). */
    void deleteByEvaluatedBy(Integer evaluatedBy);

    /** A leader's visible evaluations in one season, newest first; their rating is the average. */
    List<LeaderEvaluation> findByLeaderIdAndSeasonIdAndIsVisibleTrueOrderByDateDesc(Integer leaderId, Integer seasonId);

    long countBySeasonIdAndIsVisibleTrue(Integer seasonId);

    /** Average rating and number of evaluations per leader in a season: rows of [leaderId, avg, count]. */
    @Query("SELECT e.leader.id, AVG(e.rating), COUNT(e) FROM LeaderEvaluation e "
            + "WHERE e.seasonId = ?1 AND e.isVisible = true GROUP BY e.leader.id")
    List<Object[]> ratingsPerLeader(Integer seasonId);
}
