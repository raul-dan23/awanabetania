package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access interface for {@link Product} entities.
 * Standard CRUD operations are sufficient; no custom queries needed.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
}
