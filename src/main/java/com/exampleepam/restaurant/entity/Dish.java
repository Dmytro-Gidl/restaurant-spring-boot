package com.exampleepam.restaurant.entity;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Dish extends AbstractBaseEntity{

    private String name;
    private String description;
    private Category category;
    BigDecimal price;
    private String imageFileName;


    @OneToMany(cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            mappedBy = "dish")
    @ToString.Exclude
    private List<OrderItem> orderItems;

    public Dish(long id, String name, String description, Category category, BigDecimal price, String imagePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.imageFileName = imagePath;
    }

    @Transient
    public String getimagePath() {
        if (imageFileName == null || id == 0) return null;

        return "/dish-images/" + id + "/" + imageFileName;
    }
}
