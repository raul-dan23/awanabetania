package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Leader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Aceasta interfata ne ajuta sa lucram cu baza de date pentru Lideri.
 * Aici cautam conturile liderilor ca sa se poata loga.
 */
@Repository
public interface LeaderRepository extends JpaRepository<Leader, Integer> {

    /**
     * Cauta un lider dupa Nume si Prenume.
     * O folosim cand vrea sa se logheze in aplicatie (Login).
     */
    Optional<Leader> findByNameAndSurname(String name, String surname);

    /**
     * Gaseste toti liderii care fac parte dintr-un anumit departament.
     * De exemplu: toti liderii de la Jocuri.
     */
    List<Leader> findByDepartmentsId(Integer departmentId);

    /** Cauta un lider dupa numarul de telefon */
    Optional<Leader> findByPhoneNumber(String phoneNumber);

    Optional<Leader> findByName(String name);

    Optional<Leader> findByUsername(String username);
}