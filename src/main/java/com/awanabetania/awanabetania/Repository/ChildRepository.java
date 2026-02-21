package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Aceasta interfata ne ajuta sa lucram cu baza de date pentru Copii.
 * Nu trebuie sa scriem noi codul complicat SQL, Spring il face automat pentru noi.
 */
@Repository
public interface ChildRepository extends JpaRepository<Child, Integer> {

    /** Gaseste toti copiii care si-au cumparat manual */
    List<Child> findByHasManualTrue();

    /**
     * Cauta copii dupa nume.
     * Daca scrii "Ion", el va gasi si "Ionel" sau "Ionut" (cautare partiala).
     */
    List<Child> findByNameContaining(String name);

    Optional<Child> findByUsername(String username);
}