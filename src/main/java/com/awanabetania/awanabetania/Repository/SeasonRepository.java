package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Season;
import com.awanabetania.awanabetania.Model.SeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Integer> {

    Optional<Season> findByStatus(SeasonStatus status);

    List<Season> findAllByOrderByIdDesc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    /**
     * Closes the season only while it is still active, in a single UPDATE, so two directors
     * starting a new season at the same moment cannot both close it: the second changes no
     * row. The caller checks the returned count.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Season s SET s.status = com.awanabetania.awanabetania.Model.SeasonStatus.CLOSED, s.endDate = :endDate "
            + "WHERE s.id = :id AND s.status = com.awanabetania.awanabetania.Model.SeasonStatus.ACTIVE")
    int close(@Param("id") Integer id, @Param("endDate") LocalDate endDate);
}
