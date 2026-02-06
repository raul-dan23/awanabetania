package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.LeaderEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaderEvaluationRepository extends JpaRepository<LeaderEvaluation, Integer> {

    /** * Gaseste evaluarile dintr-o data, DAR doar pe cele care nu au fost sterse.
     */
    List<LeaderEvaluation> findByDateAndIsVisibleTrue(LocalDate date);

    /**
     * Gaseste istoricul vizibil al unui lider.
     */
    List<LeaderEvaluation> findByLeaderIdAndIsVisibleTrueOrderByDateDesc(Integer leaderId);
}