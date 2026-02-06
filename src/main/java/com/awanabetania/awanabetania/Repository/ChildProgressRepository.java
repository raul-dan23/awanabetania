package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.ChildProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Interfață care ne ajută să salvăm și să citim progresul ("Save Game"-ul) din baza de date.
 * Spring Boot implementează automat metodele precum save(), findById(), etc.
 */
@Repository
public interface ChildProgressRepository extends JpaRepository<ChildProgress, Integer> {
    // Putem adăuga metode custom aici dacă avem nevoie pe viitor.
    // De exemplu: Caută progresul după ID-ul copilului
    // ChildProgress findByChildId(Integer childId);
    // (Dar momentan avem relația directă în Child, deci nu e obligatoriu)


    @Modifying
    @Query("DELETE FROM ChildProgress cp WHERE cp.child.id = ?1")
    void deleteByChildId(Integer childId);
}