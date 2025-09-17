package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Review;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.mapper.DishMapper;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.OrderRepository;
import com.exampleepam.restaurant.repository.ReviewRepository;
import com.exampleepam.restaurant.service.recommendation.CategoryFallback;
import com.exampleepam.restaurant.service.recommendation.CollaborativePredictor;
import com.exampleepam.restaurant.service.recommendation.RatingMatrixBuilder;
import com.exampleepam.restaurant.service.recommendation.RatingMatrixBuilder.RatingData;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service providing dish recommendations for users. */
@Slf4j
@Service
public class RecommendationService {

    private static final double BLEND_ALPHA_CF = 0.6;
    private static final int CANDIDATE_MULTIPLIER = 3;
    private static final double EPS = 1e-6;

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

    /** Recommend dishes for a user using CF + MF blend with category fallback. */
    public List<DishResponseDto> getRecommendedDishes(long userId, int limit) {
        if (limit <= 0) return List.of();

        log.debug("Generating recommendations for user {} limit {}", userId, limit);

        // Load data
        final List<Review> reviews = reviewRepository.findAllWithUserAndDish();
        final List<Order> orders = orderRepository.findByStatus(Status.COMPLETED);
        log.debug("Loaded {} reviews and {} completed orders", reviews.size(), orders.size());
        if (reviews.isEmpty() && orders.isEmpty()) return List.of();

        // Build rating structures for user-based CF
        final RatingData ratingData = ratingMatrixBuilder.build(reviews, orders);
        final Map<Long, Map<Long, Double>> ratingMatrix = ratingData.matrix();
        final Map<Long, Double> targetRatings = ratingMatrix.getOrDefault(userId, Map.of());

        // --- CF branch ---
        final Map<Long, Double> cfRaw =
                Optional.ofNullable(collaborativePredictor.predict(userId, ratingData)).orElseGet(Map::of);

        // --- MF branch (biased MF) ---
        if (!factorizationService.isReady()) {
            factorizationService.train(reviews, orders);
            final double trainRmse = factorizationService.rmseOnReviews(reviews);
            log.info("Factorization trained, trainRMSE={}", trainRmse);
        }

        // Candidates: every dish seen in reviews/orders, minus user's already-rated/ordered ones
        final Set<Long> candidateIds = collectAllDishIds(reviews, orders);
        candidateIds.removeAll(targetRatings.keySet());

        // Score MF for candidates only
        final Map<Long, Double> mfScores = new HashMap<>(candidateIds.size());
        for (Long dishId : candidateIds) {
            mfScores.put(dishId, factorizationService.predict(userId, dishId));
        }

        // Blend after z-normalization to make scales comparable
        final Map<Long, Double> cfNorm = normalizeZ(cfRaw);
        final Map<Long, Double> mfNorm = normalizeZ(mfScores);
        final Map<Long, Double> blended = blend(cfNorm, mfNorm, BLEND_ALPHA_CF);

        // Ensure we never recommend already-known items (even if CF produced them)
        blended.keySet().removeAll(targetRatings.keySet());

        if (blended.isEmpty()) {
            log.debug("Using category-based fallback only (no CF/MF signals after filtering)");
            return categoryFallback.recommend(userId, targetRatings.keySet(), limit);
        }

        // Take a slightly larger candidate set for tie-breaking enrichment
        final int k = Math.min(blended.size(), Math.max(limit, limit * CANDIDATE_MULTIPLIER));
        final List<Long> topIds = blended.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Fetch, map to DTOs, decorate, and final sort by blended score + tie-breakers
        final List<Dish> dishes = dishRepository.findAllById(topIds);
        final List<DishResponseDto> dtos = dishMapper.toDishResponseDtoList(dishes);
        assignAverageRatings(dtos);
        assignReviewCounts(dtos);

        dtos.sort(Comparator
                .<DishResponseDto>comparingDouble(d -> -blended.getOrDefault(d.getId(), 0.0))
                .thenComparing(Comparator.comparingDouble(DishResponseDto::getAverageRating).reversed())
                .thenComparing(DishResponseDto::getReviewCount, Comparator.reverseOrder()));

        // Merge with category fallback if needed
        if (dtos.size() >= limit) {
            return new ArrayList<>(dtos.subList(0, limit));
        }
        final Set<Long> usedIds = dtos.stream().map(DishResponseDto::getId).collect(Collectors.toSet());
        usedIds.addAll(targetRatings.keySet());
        final List<DishResponseDto> fallback = categoryFallback.recommend(userId, usedIds, limit - dtos.size());
        log.debug("Added {} dishes from category fallback", fallback.size());

        final List<DishResponseDto> result = new ArrayList<>(dtos.size() + fallback.size());
        result.addAll(dtos);
        result.addAll(fallback);
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    // ------------ helpers ------------

    private static Set<Long> collectAllDishIds(List<Review> reviews, List<Order> orders) {
        final Set<Long> ids = new HashSet<>();
        for (Review r : reviews) {
            if (r == null || r.getDish() == null) continue;
            final Long id = r.getDish().getId();
            if (id != null) ids.add(id);
        }
        for (Order o : orders) {
            if (o == null || o.getOrderItems() == null) continue;
            for (OrderItem it : o.getOrderItems()) {
                if (it == null || it.getDish() == null) continue;
                final Long id = it.getDish().getId();
                if (id != null) ids.add(id);
            }
        }
        return ids;
    }

    private static Map<Long, Double> normalizeZ(Map<Long, Double> scores) {
        if (scores == null || scores.isEmpty()) return new HashMap<>();
        final double mean = scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double var = 0.0;
        for (double v : scores.values()) {
            final double d = v - mean;
            var += d * d;
        }
        var /= scores.size();
        double std = Math.sqrt(var);
        if (std < EPS || Double.isNaN(std) || Double.isInfinite(std)) std = 1.0;

        final Map<Long, Double> out = new HashMap<>(scores.size());
        for (Map.Entry<Long, Double> e : scores.entrySet()) {
            final double z = (e.getValue() - mean) / std;
            out.put(e.getKey(), Double.isFinite(z) ? z : 0.0);
        }
        return out;
    }

    private static Map<Long, Double> blend(Map<Long, Double> a, Map<Long, Double> b, double alpha) {
        final Map<Long, Double> out = new HashMap<>(Math.max(a.size(), b.size()));
        final Set<Long> keys = new HashSet<>(a.keySet());
        keys.addAll(b.keySet());
        for (Long id : keys) {
            final double av = a.getOrDefault(id, 0.0);
            final double bv = b.getOrDefault(id, 0.0);
            out.put(id, alpha * av + (1.0 - alpha) * bv);
        }
        return out;
    }

    private void assignAverageRatings(List<DishResponseDto> dtos) {
        for (DishResponseDto dto : dtos) {
            final Double avg = reviewRepository.getAverageRatingByDishId(dto.getId());
            dto.setAverageRating(avg == null ? 0.0 : avg);
        }
    }

    private void assignReviewCounts(List<DishResponseDto> dtos) {
        for (DishResponseDto dto : dtos) {
            final Long count = reviewRepository.countByDishId(dto.getId());
            dto.setReviewCount(count == null ? 0 : count);
        }
    }
}
