package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findDishesByCategoryAndArchivedFalse(Category category, Sort sort);

    Page<Dish> findPagedByCategoryAndArchivedFalse(Category category, Pageable pageable);

    Page<Dish> findAllByArchivedFalse(Pageable pageable);

    List<Dish> findAllByArchivedFalse(Sort sort);

    List<Dish> findByNameContainingIgnoreCaseAndArchivedFalse(String name, Sort sort);

    Page<Dish> findByNameContainingIgnoreCaseAndArchivedFalse(String name, Pageable pageable);

    Page<Dish> findByCategoryAndArchivedFalse(Category category, Pageable pageable);

    Page<Dish> findByNameContainingIgnoreCaseAndCategoryAndArchivedFalse(String name, Category category, Pageable pageable);

    Page<Dish> findAllByArchivedTrue(Pageable pageable);

    @Query("select d from Dish d where d.archived = false")
    List<Dish> findAllActiveWithIngredients();

    List<Dish> findAllByArchivedTrue(Sort sort);
}
