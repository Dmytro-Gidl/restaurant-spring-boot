package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Ingredient;
import com.exampleepam.restaurant.entity.IngredientForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IngredientForecastRepository extends JpaRepository<IngredientForecast, Long> {
    List<IngredientForecast> findByIngredientAndDateAfter(Ingredient ingredient, LocalDate date);
    void deleteByGeneratedAtBefore(LocalDate date);
    void deleteByIngredientAndGeneratedAt(Ingredient ingredient, LocalDate generatedAt);
}

