package com.exampleepam.restaurant.entity;

import lombok.Getter;

import javax.persistence.*;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "ingredient_forecast")
public class IngredientForecast extends AbstractBaseEntity {

    @ManyToOne(optional = false)
    private Ingredient ingredient;

    private LocalDate date;

    private double quantity;

    private LocalDate generatedAt;

    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public void setDate(LocalDate date) { this.date = date; }

    public void setQuantity(double quantity) { this.quantity = quantity; }

    public void setGeneratedAt(LocalDate generatedAt) { this.generatedAt = generatedAt; }
}

