package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for {@link Department} entities.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    /** Looks up a department by its exact display name. */
    Optional<Department> findByName(String name);

    /** Returns all departments where the given leader is the head. */
    List<Department> findByHeadLeaderId(Integer leaderId);
}
