package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    // Metoda care lipsea și cauza eroarea in DataInitializer
    Optional<Department> findByName(String name);

    // Metoda pentru LeaderController (caută departamente conduse de un lider)
    List<Department> findByHeadLeaderId(Integer leaderId);
}