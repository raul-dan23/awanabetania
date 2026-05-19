package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.Product;
import com.awanabetania.awanabetania.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Value("${admin.pin}")
    private String adminPin;

    private boolean isPinValid(String pin) {
        return adminPin != null && adminPin.equals(pin);
    }

    @GetMapping
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader("X-Admin-Pin") String pin,
            @RequestBody Product product) {
        if (!isPinValid(pin)) return ResponseEntity.status(403).body("Acces neautorizat.");
        product.setAvailable(true);
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Pin") String pin,
            @RequestBody Product updated) {
        if (!isPinValid(pin)) return ResponseEntity.status(403).body("Acces neautorizat.");
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        p.setName(updated.getName());
        p.setPointPrice(updated.getPointPrice());
        p.setCategory(updated.getCategory());
        p.setAvailable(updated.getAvailable());
        return ResponseEntity.ok(productRepository.save(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Integer id,
            @RequestHeader("X-Admin-Pin") String pin) {
        if (!isPinValid(pin)) return ResponseEntity.status(403).body("Acces neautorizat.");
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build();
        productRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
