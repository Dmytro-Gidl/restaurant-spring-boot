package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.dto.review.ReviewDto;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Review;
import com.exampleepam.restaurant.entity.User;
import com.exampleepam.restaurant.mapper.DishMapper;
import com.exampleepam.restaurant.mapper.ReviewMapper;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.OrderRepository;
import com.exampleepam.restaurant.repository.ReviewRepository;
import com.exampleepam.restaurant.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Slf4j
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final DishMapper dishMapper;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, OrderRepository orderRepository, DishRepository dishRepository,
                         DishMapper dishMapper, UserRepository userRepository, ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
        this.dishMapper = dishMapper;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
    }


    /**
     * Retrieve all dishes for a given order by extracting from OrderItems.
     */
    public List<DishResponseDto> getDishesForOrder(Long orderId) {
        List<Dish> dishes = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"))
                .getOrderItems()
                .stream()
                .map(OrderItem::getDish)
                .toList();
        return dishMapper.toDishResponseDtoList(dishes);
    }

    /**
     * Save a single review for a dish by a user.
     */
    public void saveReview(ReviewDto reviewDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Dish dish = dishRepository.findById(reviewDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Dish not found"));
        Review review = reviewMapper.toEntity(reviewDto);
        review.setUser(user);
        review.setDish(dish);
        reviewRepository.save(review);
    }

    /**
     * Submit multiple reviews for an order.
     */
    public void submitReviews(Long orderId, Long userId, List<ReviewDto> reviews) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        reviews.forEach(reviewDto -> {
            Dish dish = dishRepository.findById(reviewDto.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Dish not found"));
            Review review = reviewMapper.toEntity(reviewDto);
            review.setUser(user);
            review.setDish(dish);
            review.setOrder(order);
            reviewRepository.save(review);
        });
        log.info("Reviews has been saved: {}", reviews);
    }

    public boolean canAccessOrderReview(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return order.getId().equals(userId);
    }

    public List<ReviewDto> getReviewsForDish(Long dishId) {
        return reviewRepository.findAllByDishId(dishId)
                .stream()
                .map(reviewMapper::toDto)
                .toList();
    }

    public double getAverageRatingForDish(Long dishId) {
        Double avg = reviewRepository.getAverageRatingByDishId(dishId);
        return avg == null ? 0 : avg;
    }

    /**
     * Delete a review by its id.
     */
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }
}
