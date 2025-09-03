package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Ingredient;
import com.exampleepam.restaurant.entity.IngredientForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IngredientForecastRepository extends JpaRepository<IngredientForecast, Long> {
    List<IngredientForecast> findByIngredientAndDateAfter(Ingredient ingredient, LocalDate date);
    void deleteByGeneratedAtBefore(LocalDate date);
}

