package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a product available for purchase at the end-of-season fair.
 * Products are managed by directors/coordinators and displayed to all leaders
 * on the shop screen. Unavailable products are hidden from the purchase list.
 */
@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    /** Price in season points required to purchase this product. */
    @Column(name = "point_price")
    private Integer pointPrice;

    /** Display category used for grouping products in the shop UI. */
    private String category;

    /** Whether this product is currently shown on the shop screen. */
    @Column(name = "is_available")
    private Boolean available = true;
}
