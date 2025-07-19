package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findDishesByCategoryAndArchivedFalse(Category category, Sort sort);

    Page<Dish> findPagedByCategoryAndArchivedFalse(Category category, Pageable pageable);

    Page<Dish> findAllByArchivedFalse(Pageable pageable);

    List<Dish> findAllByArchivedFalse(Sort sort);
}
