package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.SeasonChildResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonChildResultRepository extends JpaRepository<SeasonChildResult, Integer> {

    @Query("SELECT r FROM SeasonChildResult r JOIN FETCH r.child WHERE r.seasonId = ?1")
    List<SeasonChildResult> findWithChildBySeasonId(Integer seasonId);

    /** Deletes a child's archived results, when their account is deleted. */
    @Modifying
    @Query("DELETE FROM SeasonChildResult r WHERE r.child.id = ?1")
    void deleteByChildId(Integer childId);
}
