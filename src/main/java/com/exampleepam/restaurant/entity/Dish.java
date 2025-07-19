package com.exampleepam.restaurant.entity;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Describes Dish entity
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Dish extends AbstractBaseEntity{

    @Column(length = 30)
    private String name;
    @Column(length = 40)
    private String description;
    @Enumerated(EnumType.STRING)
    private Category category;
    BigDecimal price;
    private String imageFileName;

    /**
     * Indicates whether this dish is archived (soft-deleted). Archived dishes
     * should not appear on the public menu but remain in the database together
     * with their reviews.
     */
    @Column(nullable = false)
    private boolean archived = false;

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
