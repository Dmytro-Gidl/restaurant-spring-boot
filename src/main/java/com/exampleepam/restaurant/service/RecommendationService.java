package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Review;
import com.exampleepam.restaurant.mapper.DishMapper;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.ReviewRepository;
import com.exampleepam.restaurant.service.FactorizationService;
import com.exampleepam.restaurant.repository.OrderRepository;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.service.recommendation.RatingMatrixBuilder;
import com.exampleepam.restaurant.service.recommendation.RatingMatrixBuilder.RatingData;
import com.exampleepam.restaurant.service.recommendation.CollaborativePredictor;
import com.exampleepam.restaurant.service.recommendation.CategoryFallback;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service providing dish recommendations for users.
 */
@Slf4j
@Service
public class RecommendationService {

    private final DishRepository dishRepository;
    private final DishMapper dishMapper;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final FactorizationService factorizationService;
    private final RatingMatrixBuilder ratingMatrixBuilder;
    private final CollaborativePredictor collaborativePredictor;
    private final CategoryFallback categoryFallback;

    @Autowired
    public RecommendationService(DishRepository dishRepository,
                                  DishMapper dishMapper,
                                  ReviewRepository reviewRepository,
                                  OrderRepository orderRepository,
                                  FactorizationService factorizationService,
                                  RatingMatrixBuilder ratingMatrixBuilder,
                                  CollaborativePredictor collaborativePredictor,
                                  CategoryFallback categoryFallback) {
        this.dishRepository = dishRepository;
        this.dishMapper = dishMapper;
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.factorizationService = factorizationService;
        this.ratingMatrixBuilder = ratingMatrixBuilder;
        this.collaborativePredictor = collaborativePredictor;
        this.categoryFallback = categoryFallback;
    }

    /**
     * Recommend dishes for a user using a simple collaborative filtering
     * approach. Ratings of other users are used to predict how much the
     * specified user might like a dish. Only dishes the user has not rated are
     * considered and the resulting list is ordered by the predicted score.
     *
     * @param userId id of the user for which recommendations are generated
     * @param limit  maximum number of dishes to return
     * @return list of recommended dishes, possibly empty
     */
    public List<DishResponseDto> getRecommendedDishes(long userId, int limit) {
        log.debug("Generating recommendations for user {} limit {}", userId, limit);
        List<Review> reviews = reviewRepository.findAllWithUserAndDish();
        // Fetch completed orders without using LocalDateTime.MIN which can cause
        // serialisation issues with some JDBC drivers. All completed orders are
        // included, as the recommendation algorithms handle their own recency
        // weighting.
        List<Order> orders = orderRepository.findByStatus(Status.COMPLETED);
        log.debug("Loaded {} reviews and {} completed orders for recommendation", reviews.size(), orders.size());
        if (reviews.isEmpty() && orders.isEmpty()) {
            log.debug("No data available for recommendations");
            return List.of();
        }

        RatingData ratingData = ratingMatrixBuilder.build(reviews, orders);
        Map<Long, Map<Long, Double>> ratingMatrix = ratingData.matrix();
        Map<Long, Double> userMeans = ratingData.means();
        Map<Long, Double> targetRatings = ratingMatrix.getOrDefault(userId, Map.of());
        Map<Long, Double> predictedRatings = collaborativePredictor.predict(userId, ratingData);
        if (!factorizationService.isReady()) {
            factorizationService.train(reviews, orders);
            log.debug("Trained factorization model");
        }
        for (Long dishId : ratingMatrix.keySet()) {
            // no-op, ensures factor vectors for known dishes/users
        }
        java.util.Set<Long> allDishIds = new java.util.HashSet<>();
        for (Review r : reviews) allDishIds.add(r.getDish().getId());
        for (Long dishId : allDishIds) {
            if (targetRatings.containsKey(dishId)) continue;
            double pred = factorizationService.predict(userId, dishId);
            predictedRatings.merge(dishId, pred, (a,b) -> (a+b)/2);
        }
        log.debug("Predicted ratings for {} dishes", predictedRatings.size());
        if (predictedRatings.isEmpty()) {
            log.debug("Prediction set empty – falling back to category preferences");
            return categoryFallback.recommend(userId, targetRatings.keySet(), limit);
        }

        Set<Long> dishIds = predictedRatings.keySet();
        List<Dish> dishes = dishRepository.findAllById(dishIds);
        List<DishResponseDto> dtos = dishMapper.toDishResponseDtoList(dishes);
        assignAverageRatings(dtos);
        assignReviewCounts(dtos);

        dtos.sort(Comparator.comparingDouble(d -> -predictedRatings.getOrDefault(d.getId(), 0.0)));
        if (dtos.size() >= limit) {
            List<DishResponseDto> result = dtos.subList(0, limit);
            log.debug("Returning {} recommendations", result.size());
            return result;
        }

        // If collaborative filtering produced fewer dishes than needed, fill up
        // the remainder using the user's preferred categories.
        Set<Long> usedIds = new java.util.HashSet<>();
        for (DishResponseDto dto : dtos) {
            usedIds.add(dto.getId());
        }
        usedIds.addAll(targetRatings.keySet());
        List<DishResponseDto> fallback = categoryFallback.recommend(userId, usedIds, limit - dtos.size());
        dtos.addAll(fallback);
        log.debug("Returning {} recommendations after fallback", dtos.size());
        return dtos;
    }

    private void assignAverageRatings(List<DishResponseDto> dtos) {
        for (DishResponseDto dto : dtos) {
            Double avg = reviewRepository.getAverageRatingByDishId(dto.getId());
            dto.setAverageRating(avg == null ? 0.0 : avg);
        }
    }

    private void assignReviewCounts(List<DishResponseDto> dtos) {
        for (DishResponseDto dto : dtos) {
            Long count = reviewRepository.countByDishId(dto.getId());
            dto.setReviewCount(count == null ? 0 : count);
        }
    }
}
