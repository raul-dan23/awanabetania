package com.awanabetania.awanabetania.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(name = "point_price")
    private Integer pointPrice;

    private String category;

    @Column(name = "is_available")
    private Boolean available = true;
}
