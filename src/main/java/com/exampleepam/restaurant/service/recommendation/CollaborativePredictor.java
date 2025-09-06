package com.exampleepam.restaurant.service.recommendation;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CollaborativePredictor {

    public Map<Long, Double> predict(long userId, RatingMatrixBuilder.RatingData data) {
        Map<Long, Map<Long, Double>> ratingMatrix = data.matrix();
        Map<Long, Double> userMeans = data.means();
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
            if (overlap == 0) {
                log.debug("No overlap between user {} and {}", userId, otherUserId);
                continue;
            }
            double sim = cosineSimilarity(target, other);
            double weight = overlap / (overlap + 5.0);
            sim *= weight;
            if (sim <= 0) {
                log.debug("Non-positive similarity between user {} and {}", userId, otherUserId);
                continue;
            }
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
        log.debug("Collaborative predictor produced {} dish scores for user {}", preds.size(), userId);
        if (preds.isEmpty()) {
            log.debug("Collaborative predictor produced no scores for user {}", userId);
        }
        return preds;
    }

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
}
