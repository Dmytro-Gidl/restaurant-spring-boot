package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.DishForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DishForecastRepository extends JpaRepository<DishForecast, Long> {
    List<DishForecast> findByDishAndDateAfter(Dish dish, LocalDate date);
    void deleteByGeneratedAtBefore(LocalDate date);
}

