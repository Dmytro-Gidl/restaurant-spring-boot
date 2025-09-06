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
import java.util.HashSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service providing dish recommendations for users.
 */
@Slf4j
@Service
public class RecommendationService {

    private static final double BLEND_ALPHA_CF = 0.6; // вес коллаборативных предсказаний

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
     * Recommend dishes for a user using a simple collaborative filtering approach.
     */
    public List<DishResponseDto> getRecommendedDishes(long userId, int limit) {
        if (limit <= 0) return List.of();

        log.debug("Generating recommendations for user {} limit {}", userId, limit);
        List<Review> reviews = reviewRepository.findAllWithUserAndDish();
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

        Map<Long, Double> cfRaw = collaborativePredictor.predict(userId, ratingData);
        Map<Long, Double> predictedRatings = new HashMap<>(cfRaw == null ? Map.of() : cfRaw);

        if (!factorizationService.isReady()) {
            factorizationService.train(reviews, orders);
            log.debug("Trained factorization model");
        }

        Set<Long> allDishIds = new HashSet<>();
        for (Review r : reviews) {
            if (r.getDish() != null) allDishIds.add(r.getDish().getId());
        }
        for (Order o : orders) {
            if (o.getOrderItems() == null) continue;
            for (OrderItem it : o.getOrderItems()) {
                if (it.getDish() != null) allDishIds.add(it.getDish().getId());
            }
        }

        Map<Long, Double> mfScores = new HashMap<>();
        for (Long dishId : allDishIds) {
            if (targetRatings.containsKey(dishId)) continue; // не предлагаем уже оценённые
            double pred = factorizationService.predict(userId, dishId);
            mfScores.put(dishId, pred);
        }

        Map<Long, Double> cfNorm = normalizeZ(predictedRatings);
        Map<Long, Double> mfNorm = normalizeZ(mfScores);
        Map<Long, Double> blended = blend(cfNorm, mfNorm, BLEND_ALPHA_CF);

        predictedRatings.clear();
        predictedRatings.putAll(blended);

        predictedRatings.keySet().removeAll(targetRatings.keySet());

        log.debug("Predicted ratings for {} dishes", predictedRatings.size());
        if (predictedRatings.isEmpty()) {
            log.debug("Using category-based fallback only");
            return categoryFallback.recommend(userId, targetRatings.keySet(), limit);
        } else {
            log.debug("Using collaborative and factorization predictions");
        }

        int k = Math.min(predictedRatings.size(), Math.max(limit, limit * 3));
        List<Long> topIds = predictedRatings.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Dish> dishes = dishRepository.findAllById(topIds);
        List<DishResponseDto> dtos = dishMapper.toDishResponseDtoList(dishes);
        assignAverageRatings(dtos);
        assignReviewCounts(dtos);

        dtos.sort(Comparator
                .<DishResponseDto>comparingDouble(d -> -predictedRatings.getOrDefault(d.getId(), 0.0))
                .thenComparing(Comparator.comparingDouble(DishResponseDto::getAverageRating).reversed())
                .thenComparing(DishResponseDto::getReviewCount, Comparator.reverseOrder())
        );

        List<DishResponseDto> result;
        if (dtos.size() >= limit) {
            result = new ArrayList<>(dtos.subList(0, limit));
        } else {
            Set<Long> usedIds = dtos.stream().map(DishResponseDto::getId).collect(Collectors.toSet());
            usedIds.addAll(targetRatings.keySet());
            List<DishResponseDto> fallback = categoryFallback.recommend(userId, usedIds, limit - dtos.size());
            log.debug("Added {} dishes from category fallback", fallback.size());
            dtos.addAll(fallback);
            result = dtos.size() > limit ? new ArrayList<>(dtos.subList(0, limit)) : dtos;
        }
        log.debug("Returning {} recommendations", result.size());
        return result;
    }

    private static Map<Long, Double> normalizeZ(Map<Long, Double> scores) {
        if (scores == null || scores.isEmpty()) return new HashMap<>();
        double mean = scores.values().stream().mapToDouble(d -> d).average().orElse(0.0);
        double var = scores.values().stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0.0);
        double std = Math.sqrt(var);
        if (std < 1e-6) std = 1.0;
        Map<Long, Double> out = new HashMap<>(scores.size());
        for (Map.Entry<Long, Double> e : scores.entrySet()) {
            out.put(e.getKey(), (e.getValue() - mean) / std);
        }
        return out;
    }

    private static Map<Long, Double> blend(Map<Long, Double> a, Map<Long, Double> b, double alpha) {
        Map<Long, Double> out = new HashMap<>();
        Set<Long> keys = new HashSet<>(a.keySet());
        keys.addAll(b.keySet());
        for (Long id : keys) {
            double av = a.getOrDefault(id, 0.0);
            double bv = b.getOrDefault(id, 0.0);
            out.put(id, alpha * av + (1.0 - alpha) * bv);
        }
        return out;
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
