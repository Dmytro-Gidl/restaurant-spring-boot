package com.exampleepam.restaurant.service.recommendation;

import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Review;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class RatingMatrixBuilder {

    public RatingData build(List<Review> reviews, List<Order> orders) {
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

    public record RatingData(Map<Long, Map<Long, Double>> matrix, Map<Long, Double> means) {}
}
