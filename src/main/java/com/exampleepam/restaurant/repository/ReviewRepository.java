package com.exampleepam.restaurant.repository;

import com.exampleepam.restaurant.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByDishId(Long dishId);

    List<Review> findAllByUserId(Long userId);

    Optional<Review> findByUserIdAndDishId(Long userId, Long dishId);

    @Query("select avg(r.rating) from Review r where r.dish.id = :dishId")
    Double getAverageRatingByDishId(@Param("dishId") Long dishId);
}
