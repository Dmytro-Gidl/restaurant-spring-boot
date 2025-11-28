package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.DishForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DishForecastRepository extends JpaRepository<DishForecast, Long> {
    List<DishForecast> findByDishAndDateAfter(Dish dish, LocalDate date);
    void deleteByGeneratedAtBefore(LocalDate date);
    void deleteByDishAndGeneratedAt(Dish dish, LocalDate generatedAt);
}

