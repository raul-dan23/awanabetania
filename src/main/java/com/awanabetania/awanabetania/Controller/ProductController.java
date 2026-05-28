package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Product;
import com.awanabetania.awanabetania.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages the product catalog for the end-of-season fair.
 * Read operations are public; create, update, and delete require the admin PIN
 * supplied via the {@code X-Admin-Pin} request header.
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Value("${admin.pin}")
    private String adminPin;

    /** Returns {@code true} if the supplied PIN matches the configured admin PIN. */
    private boolean isPinValid(String pin) {
        return adminPin != null && adminPin.equals(pin);
    }

    /**
     * Returns all products (available and unavailable).
     *
     * @return list of all {@link Product} entities
     */
    @GetMapping
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    /**
     * Creates a new product. New products are available by default.
     *
     * @param pin     admin PIN from the {@code X-Admin-Pin} header
     * @param product product data from the request body
     * @return 200 with the saved entity; 403 on invalid PIN
     */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader("X-Admin-Pin") String pin,
            @RequestBody Product product) {
        if (!isPinValid(pin)) return ResponseEntity.status(403).body("Unauthorized.");
        product.setAvailable(true);
        return ResponseEntity.ok(productRepository.save(product));
    }

    /**
     * Updates an existing product.
     *
     * @param id      the product's primary key
     * @param pin     admin PIN from the {@code X-Admin-Pin} header
     * @param updated updated product fields from the request body
     * @return 200 with the updated entity; 403 on invalid PIN; 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Pin") String pin,
            @RequestBody Product updated) {
        if (!isPinValid(pin)) return ResponseEntity.status(403).body("Unauthorized.");
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        p.setName(updated.getName());
        p.setPointPrice(updated.getPointPrice());
        p.setCategory(updated.getCategory());
        p.setAvailable(updated.getAvailable());
        return ResponseEntity.ok(productRepository.save(p));
    }

    /**
     * Permanently deletes a product.
     *
     * @param id  the product's primary key
     * @param pin admin PIN from the {@code X-Admin-Pin} header
     * @return 200 on success; 403 on invalid PIN; 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Pin") String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(403).body("Unauthorized.");
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build();
        productRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
