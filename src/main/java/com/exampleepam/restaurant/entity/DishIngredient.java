package com.exampleepam.restaurant.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Join entity linking dishes with their ingredients and required quantity.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class DishIngredient extends AbstractBaseEntity {

    @ManyToOne(optional = false)
    private Dish dish;

    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Ingredient ingredient;

    /**
     * Quantity of the ingredient required for one dish portion.
     */
    @Column(nullable = false)
    private int quantity;
}
