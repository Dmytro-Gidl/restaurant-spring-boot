package com.exampleepam.restaurant.entity;

import lombok.Getter;

import javax.persistence.*;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "dish_forecast")
public class DishForecast extends AbstractBaseEntity {

    @ManyToOne(optional = false)
    private Dish dish;

    private LocalDate date;

    private int quantity;

    private LocalDate generatedAt;

    public void setDish(Dish dish) { this.dish = dish; }

    public void setDate(LocalDate date) { this.date = date; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    public void setGeneratedAt(LocalDate generatedAt) { this.generatedAt = generatedAt; }
}

