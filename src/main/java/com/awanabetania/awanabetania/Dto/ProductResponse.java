package com.awanabetania.awanabetania.Dto;

import com.awanabetania.awanabetania.Model.Product;

/** A shop product. */
public record ProductResponse(Integer id, String name, Integer pointPrice, String category, Boolean available) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPointPrice(), p.getCategory(), p.getAvailable());
    }
}
