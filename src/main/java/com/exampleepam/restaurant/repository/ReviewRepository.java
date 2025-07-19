package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByDishId(Long dishId);

    Page<Review> findAllByDishId(Long dishId, Pageable pageable);

    List<Review> findAllByUserId(Long userId);

    Optional<Review> findByUserIdAndDishId(Long userId, Long dishId);

    @Query("select avg(r.rating) from Review r where r.dish.id = :dishId")
    Double getAverageRatingByDishId(@Param("dishId") Long dishId);

    @Query("select count(r) from Review r where r.dish.id = :dishId")
    Long countByDishId(@Param("dishId") Long dishId);
}
