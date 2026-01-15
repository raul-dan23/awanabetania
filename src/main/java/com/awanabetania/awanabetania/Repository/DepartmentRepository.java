package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Aceasta interfata ne ajuta sa lucram cu baza de date pentru Departamente.
 * Aici putem cauta departamentele dupa nume sau ID.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    /**
     * Cauta un departament dupa numele lui (ex: Jocuri, Secretariat).
     * Folosim Optional pentru ca e posibil sa nu existe niciun departament cu acel nume.
     */
    Optional<Department> findByName(String name);
}