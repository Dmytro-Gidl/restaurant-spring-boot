package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for Ingredient entity.
 */
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByNameIgnoreCase(String name);
}
