package com.exampleepam.restaurant.service.recommendation;

import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Review;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RatingMatrixBuilder {

    public RatingData build(List<Review> reviews, List<Order> orders) {
        log.debug("Building rating matrix from {} reviews and {} orders", reviews.size(), orders.size());
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        Map<Long, List<Integer>> temp = new HashMap<>();
        Map<Long, Double> means = new HashMap<>();
        Set<Long> reviewUsers = new HashSet<>();
        for (Review r : reviews) {
            long u = r.getUser().getId();
            reviewUsers.add(u);
            temp.computeIfAbsent(u, k -> new ArrayList<>()).add(r.getRating());
            matrix.computeIfAbsent(u, k -> new HashMap<>())
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
        Set<Long> orderUsers = new HashSet<>();
        for (Order o : orders) {
            orderUsers.add(o.getUser().getId());
        }
        orderUsers.removeAll(reviewUsers);
        if (!orderUsers.isEmpty()) {
            log.debug("Users with orders but no reviews: {}", orderUsers);
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
        int dishCount = (int) matrix.values().stream().flatMap(m -> m.keySet().stream()).distinct().count();
        log.debug("Rating matrix built for {} users and {} dishes", matrix.size(), dishCount);
        return new RatingData(matrix, means);
    }

    public record RatingData(Map<Long, Map<Long, Double>> matrix, Map<Long, Double> means) {}
}
