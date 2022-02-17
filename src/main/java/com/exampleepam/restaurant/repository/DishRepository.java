package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findDishesByCategory(Category category, Sort sort);

    Page<Dish> findPagedByCategory(Category category, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM Dish d WHERE d.id=?1")
    Integer deleteDishById(long id);
}
