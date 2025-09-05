package com.exampleepam.restaurant.entity;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dish_forecast")
public class DishForecast extends AbstractBaseEntity {

    @ManyToOne(optional = false)
    private Dish dish;

    private LocalDate date;

    private int quantity;

    private LocalDate generatedAt;

    public Dish getDish() { return dish; }
    public void setDish(Dish dish) { this.dish = dish; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDate getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDate generatedAt) { this.generatedAt = generatedAt; }
}

