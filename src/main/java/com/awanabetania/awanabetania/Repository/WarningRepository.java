package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Warning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Aceasta interfata ne ajuta sa lucram cu baza de date pentru Avertismente (Pedepse).
 * Aici cautam sanctiunile primite de un copil.
 */
@Repository
public interface WarningRepository extends JpaRepository<Warning, Integer> {

    /**
     * Aceasta comanda speciala (Query) cauta toate pedepsele unui anumit copil.
     * Le afiseaza de la cea mai recenta la cea mai veche.
     */
    @Query("SELECT w FROM Warning w WHERE w.child.id = :childId ORDER BY w.id DESC")
    List<Warning> findByChildIdOrderByIdDesc(@Param("childId") Integer childId);
}