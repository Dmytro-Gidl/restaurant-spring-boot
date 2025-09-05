package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.Category;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Service providing dish recommendations for users.
 */
@Service
public class RecommendationService {

    private final DishRepository dishRepository;
    private final DishMapper dishMapper;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final FactorizationService factorizationService;

    @Autowired
    public RecommendationService(DishRepository dishRepository,
                                  DishMapper dishMapper,
                                  ReviewRepository reviewRepository,
                                  OrderRepository orderRepository,
                                  FactorizationService factorizationService) {
        this.dishRepository = dishRepository;
        this.dishMapper = dishMapper;
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.factorizationService = factorizationService;
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
        List<Review> reviews = reviewRepository.findAllWithUserAndDish();
        List<Order> orders = orderRepository.findByStatusAndCreationDateTimeAfter(Status.COMPLETED, java.time.LocalDateTime.MIN);
        if (reviews.isEmpty() && orders.isEmpty()) {
            return List.of();
        }

        RatingData ratingData = buildRatingMatrix(reviews, orders);
        Map<Long, Map<Long, Double>> ratingMatrix = ratingData.matrix;
        Map<Long, Double> userMeans = ratingData.means;
        Map<Long, Double> targetRatings = ratingMatrix.getOrDefault(userId, Map.of());
        Map<Long, Double> predictedRatings = predictRatings(userId, ratingMatrix, userMeans);
        if (!factorizationService.isReady()) {
            factorizationService.train(reviews, orders);
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
        if (predictedRatings.isEmpty()) {
            return fallbackByCategory(userId, targetRatings.keySet(), limit);
        }

        Set<Long> dishIds = predictedRatings.keySet();
        List<Dish> dishes = dishRepository.findAllById(dishIds);
        List<DishResponseDto> dtos = dishMapper.toDishResponseDtoList(dishes);
        assignAverageRatings(dtos);
        assignReviewCounts(dtos);

        dtos.sort(Comparator.comparingDouble(d -> -predictedRatings.getOrDefault(d.getId(), 0.0)));
        if (dtos.size() >= limit) {
            return dtos.subList(0, limit);
        }

        // If collaborative filtering produced fewer dishes than needed, fill up
        // the remainder using the user's preferred categories.
        Set<Long> usedIds = new java.util.HashSet<>();
        for (DishResponseDto dto : dtos) {
            usedIds.add(dto.getId());
        }
        usedIds.addAll(targetRatings.keySet());
        List<DishResponseDto> fallback = fallbackByCategory(userId, usedIds, limit - dtos.size());
        dtos.addAll(fallback);
        return dtos;
    }

    /** Build user->dish rating matrix from review list. */
    private RatingData buildRatingMatrix(List<Review> reviews, List<Order> orders) {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        Map<Long, List<Integer>> temp = new HashMap<>();
        Map<Long, Double> means = new HashMap<>();
        for (Review r : reviews) {
            temp.computeIfAbsent(r.getUser().getId(), k -> new ArrayList<>()).add(r.getRating());
            matrix.computeIfAbsent(r.getUser().getId(), k -> new HashMap<>())
                .put(r.getDish().getId(), (double) r.getRating());
        }
        for (Order o : orders) {
            long userId = o.getUser().getId();
            for (OrderItem item : o.getOrderItems()) {
                long dishId = item.getDish().getId();
                if (matrix.getOrDefault(userId, Map.of()).containsKey(dishId)) {
                    continue;
                }
                temp.computeIfAbsent(userId, k -> new ArrayList<>()).add(1);
                matrix.computeIfAbsent(userId, k -> new HashMap<>()).put(dishId, 1.0);
            }
        }
        for (var e : matrix.entrySet()) {
            long u = e.getKey();
            double mean = temp.get(u).stream().mapToInt(Integer::intValue).average().orElse(0);
            means.put(u, mean);
            Map<Long, Double> userRatings = e.getValue();
            for (var d : userRatings.entrySet()) {
                d.setValue(d.getValue() - mean);
            }
        }
        return new RatingData(matrix, means);
    }

    /**
     * Predict ratings for dishes the user has not rated using a cosine
     * similarity weighted average of other users' ratings.
     */
    private Map<Long, Double> predictRatings(long userId, Map<Long, Map<Long, Double>> ratingMatrix,
                                             Map<Long, Double> userMeans) {
        Map<Long, Double> target = ratingMatrix.getOrDefault(userId, Map.of());
        double targetMean = userMeans.getOrDefault(userId, 0.0);
        Map<Long, Double> scoreSums = new HashMap<>();
        Map<Long, Double> similaritySums = new HashMap<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : ratingMatrix.entrySet()) {
            long otherUserId = entry.getKey();
            if (otherUserId == userId) continue;
            Map<Long, Double> other = entry.getValue();
            int overlap = 0;
            for (Long d : target.keySet()) {
                if (other.containsKey(d)) overlap++;
            }
            if (overlap == 0) continue;
            double sim = cosineSimilarity(target, other);
            double weight = overlap / (overlap + 5.0);
            sim *= weight;
            if (sim <= 0) continue;
            for (Map.Entry<Long, Double> dr : other.entrySet()) {
                long dishId = dr.getKey();
                if (target.containsKey(dishId)) continue;
                scoreSums.merge(dishId, sim * dr.getValue(), Double::sum);
                similaritySums.merge(dishId, sim, Double::sum);
            }
        }

        Map<Long, Double> preds = new HashMap<>();
        for (Long dishId : scoreSums.keySet()) {
            double norm = similaritySums.getOrDefault(dishId, 1.0);
            preds.put(dishId, targetMean + scoreSums.get(dishId) / norm);
        }
        return preds;
    }

    private List<DishResponseDto> fallbackByCategory(long userId, Set<Long> excludeIds, int limit) {
        // use a mutable copy so callers can pass immutable sets
        Set<Long> excluded = new java.util.HashSet<>(excludeIds);
        List<DishResponseDto> result = new ArrayList<>();
        List<Object[]> preferredCats = reviewRepository.findPreferredCategories(userId);
        for (Object[] row : preferredCats) {
            Category cat = (Category) row[0];
            List<Dish> dishes = dishRepository.findDishesByCategoryAndArchivedFalse(cat, Sort.by("name"));
            List<DishResponseDto> dtos = dishMapper.toDishResponseDtoList(dishes);
            assignAverageRatings(dtos);
            assignReviewCounts(dtos);
            dtos.sort(Comparator.comparing(DishResponseDto::getAverageRating).reversed());
            for (DishResponseDto dto : dtos) {
                if (excluded.contains(dto.getId())) {
                    continue;
                }
                result.add(dto);
                excluded.add(dto.getId());
                if (result.size() >= limit) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Compute cosine similarity between two rating vectors.
     */
    private double cosineSimilarity(Map<Long, Double> a, Map<Long, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (var e : a.entrySet()) {
            double ra = e.getValue();
            normA += ra * ra;
            Double rb = b.get(e.getKey());
            if (rb != null) {
                dot += ra * rb;
            }
        }
        for (double rb : b.values()) {
            normB += rb * rb;
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record RatingData(Map<Long, Map<Long, Double>> matrix, Map<Long, Double> means) {}

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
