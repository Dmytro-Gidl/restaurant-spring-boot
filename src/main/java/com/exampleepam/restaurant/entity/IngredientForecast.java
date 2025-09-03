package com.exampleepam.restaurant.entity;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ingredient_forecast")
public class IngredientForecast extends AbstractBaseEntity {

    @ManyToOne(optional = false)
    private Ingredient ingredient;

    private LocalDate date;

    private double quantity;

    private LocalDate generatedAt;

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public LocalDate getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDate generatedAt) { this.generatedAt = generatedAt; }
}

