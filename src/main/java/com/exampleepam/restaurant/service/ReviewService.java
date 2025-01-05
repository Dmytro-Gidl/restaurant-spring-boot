package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.ReviewDto;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Review;
import com.exampleepam.restaurant.entity.User;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.ReviewRepository;
import com.exampleepam.restaurant.repository.UserRepository;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final DishRepository dishRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository,
                         DishRepository dishRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.dishRepository = dishRepository;
        this.userRepository = userRepository;
    }

    public void saveReview(ReviewDto reviewDto, AuthenticatedUser authenticatedUser) {
        long userId = authenticatedUser.getUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Dish dish = dishRepository.findById(reviewDto.getDishId()).orElseThrow(() -> new RuntimeException("Dish not found"));

        Review review = new Review();
        review.setDish(dish);
        review.setUser(user);
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        reviewRepository.save(review);
    }

    public double getAverageRating(Long dishId) {
        List<Review> reviews = reviewRepository.findAllByDishId(dishId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Review r : reviews) {
            sum += r.getRating();
        }
        return sum / reviews.size();
    }

    public List<Review> getReviewsByDish(Long dishId) {
        return reviewRepository.findAllByDishId(dishId);
    }
}

