package com.exampleepam.restaurant.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Setter
@Getter
@Entity
@Table(name = "ingredient_forecast")
public class IngredientForecast extends AbstractBaseEntity {

    @ManyToOne(optional = false)
    private Ingredient ingredient;

    private LocalDate date;

    private double quantity;

    private LocalDate generatedAt;

}
