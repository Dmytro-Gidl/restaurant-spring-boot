package com.exampleepam.restaurant.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Describes OrderItem entity
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem extends AbstractBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id")
    private Dish dish;
    private Integer quantity;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    public OrderItem(Dish dish, Integer quantity) {
        this.dish = dish;
        this.quantity = quantity;
    }
}
