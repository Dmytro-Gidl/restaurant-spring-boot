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

    /**
     * Fetch all reviews together with their associated user and dish.
     * This helps avoid lazy loading overhead when performing recommendation
     * calculations.
     */
    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.dish")
    List<Review> findAllWithUserAndDish();

    /**
     * Return categories ordered by the average rating that the specified user
     * gave to dishes of those categories. The first element in the Object[] is
     * a {@link com.exampleepam.restaurant.entity.Category} and the second is the
     * average rating as {@link Double}.
     */
    @Query("SELECT r.dish.category, AVG(r.rating) as avgRating " +
            "FROM Review r WHERE r.user.id = :userId " +
            "GROUP BY r.dish.category ORDER BY avgRating DESC")
    List<Object[]> findPreferredCategories(@Param("userId") Long userId);
}
