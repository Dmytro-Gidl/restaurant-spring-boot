package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Review;
import com.exampleepam.restaurant.mapper.DishMapper;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.ReviewRepository;
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

    @Autowired
    public RecommendationService(DishRepository dishRepository,
                                  DishMapper dishMapper,
                                  ReviewRepository reviewRepository) {
        this.dishRepository = dishRepository;
        this.dishMapper = dishMapper;
        this.reviewRepository = reviewRepository;
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
        if (reviews.isEmpty()) {
            return List.of();
        }

        // build user -> (dish -> rating) map
        Map<Long, Map<Long, Integer>> ratingMatrix = new HashMap<>();
        for (Review r : reviews) {
            ratingMatrix
                .computeIfAbsent(r.getUser().getId(), k -> new HashMap<>())
                .put(r.getDish().getId(), r.getRating());
        }

        Map<Long, Integer> targetRatings = ratingMatrix.getOrDefault(userId, Map.of());

        Map<Long, Double> scoreSums = new HashMap<>();
        Map<Long, Double> similaritySums = new HashMap<>();

        for (Map.Entry<Long, Map<Long, Integer>> entry : ratingMatrix.entrySet()) {
            long otherUserId = entry.getKey();
            if (otherUserId == userId) {
                continue;
            }
            Map<Long, Integer> otherRatings = entry.getValue();
            double similarity = cosineSimilarity(targetRatings, otherRatings);
            if (similarity <= 0) {
                continue;
            }
            for (Map.Entry<Long, Integer> dishRating : otherRatings.entrySet()) {
                long dishId = dishRating.getKey();
                if (targetRatings.containsKey(dishId)) {
                    continue; // user already rated this dish
                }
                scoreSums.merge(dishId, similarity * dishRating.getValue(), Double::sum);
                similaritySums.merge(dishId, similarity, Double::sum);
            }
        }

        if (scoreSums.isEmpty()) {
            return fallbackByCategory(userId, targetRatings.keySet(), limit);
        }

        Map<Long, Double> predictedRatings = new HashMap<>();
        for (Long dishId : scoreSums.keySet()) {
            double norm = similaritySums.getOrDefault(dishId, 1.0);
            predictedRatings.put(dishId, scoreSums.get(dishId) / norm);
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
    private double cosineSimilarity(Map<Long, Integer> a, Map<Long, Integer> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (var e : a.entrySet()) {
            int ra = e.getValue();
            normA += ra * ra;
            Integer rb = b.get(e.getKey());
            if (rb != null) {
                dot += ra * rb;
            }
        }
        for (int rb : b.values()) {
            normB += rb * rb;
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
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
