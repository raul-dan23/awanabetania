package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Dto.ProductRequest;
import com.awanabetania.awanabetania.Dto.ProductResponse;
import com.awanabetania.awanabetania.Security.AdminPinVerifier;
import com.awanabetania.awanabetania.Service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The fair's product catalog. Any leader can list it; changes need the admin PIN. */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final AdminPinVerifier pinVerifier;

    @GetMapping
    public List<ProductResponse> all() {
        return productService.all();
    }

    /** 400 invalid, 403 wrong PIN. */
    @PostMapping
    public ProductResponse create(@RequestHeader(value = "X-Admin-Pin", required = false) String pin,
                                  @Valid @RequestBody ProductRequest request) {
        pinVerifier.verify(pin);
        return productService.create(request);
    }

    /** 400 invalid, 403 wrong PIN, 404 unknown product. */
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Integer id,
                                  @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
                                  @Valid @RequestBody ProductRequest request) {
        pinVerifier.verify(pin);
        return productService.update(id, request);
    }

    /** 403 wrong PIN, 404 unknown product. */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id,
                       @RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        pinVerifier.verify(pin);
        productService.delete(id);
    }
}
