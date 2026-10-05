package com.awanabetania.awanabetania.Service;

import com.awanabetania.awanabetania.Dto.ProductRequest;
import com.awanabetania.awanabetania.Dto.ProductResponse;
import com.awanabetania.awanabetania.Exception.ApiException;
import com.awanabetania.awanabetania.Model.Product;
import com.awanabetania.awanabetania.Repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The fair's product catalog. */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> all() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    /** New products start out available. */
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        product.setAvailable(true);
        return ProductResponse.from(productRepository.save(product));
    }

    /** @throws ApiException 404 for an unknown product */
    @Transactional
    public ProductResponse update(Integer id, ProductRequest request) {
        Product product = find(id);
        apply(product, request);
        if (request.available() != null) product.setAvailable(request.available());
        return ProductResponse.from(productRepository.save(product));
    }

    /** @throws ApiException 404 for an unknown product */
    @Transactional
    public void delete(Integer id) {
        productRepository.delete(find(id));
    }

    private Product find(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found."));
    }

    private static void apply(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setPointPrice(request.pointPrice());
        product.setCategory(request.category());
    }
}
