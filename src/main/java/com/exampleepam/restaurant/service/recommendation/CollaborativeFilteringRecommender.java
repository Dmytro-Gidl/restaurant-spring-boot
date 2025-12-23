package com.exampleepam.restaurant.service.recommendation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CollaborativeFilteringRecommender {

    // tune as needed
    private static final double OVERLAP_SHRINK = 5.0;

    // if you know your rating scale, clamp; otherwise remove
    private static final double MIN_RATING = 0.0;
    private static final double MAX_RATING = 5.0;

    public Map<Long, Double> predict(long userId, RatingMatrixBuilder.RatingData data) {
        Map<Long, Map<Long, Double>> matrix = data.matrix();
        Map<Long, Double> means = data.means();

        Map<Long, Double> target = matrix.get(userId);
        if (target == null || target.isEmpty()) {
            return Map.of();
        }
        double targetMean = means.getOrDefault(userId, 0.0);

        Map<Long, Double> numer = new HashMap<>();
        Map<Long, Double> denom = new HashMap<>();

        for (Map.Entry<Long, Map<Long, Double>> e : matrix.entrySet()) {
            long otherUserId = e.getKey();
            if (otherUserId == userId) continue;

            Map<Long, Double> other = e.getValue();
            if (other == null || other.isEmpty()) continue;

            double otherMean = means.getOrDefault(otherUserId, 0.0);

            Sim sim = centeredCosine(target, targetMean, other, otherMean);
            if (sim.overlap == 0) continue;

            // shrink similarity based on overlap
            double weight = sim.overlap / (sim.overlap + OVERLAP_SHRINK);
            double s = sim.value * weight;
            if (s <= 0.0) continue;

            // contribute to items user hasn't rated
            for (Map.Entry<Long, Double> r : other.entrySet()) {
                long dishId = r.getKey();
                if (target.containsKey(dishId)) continue;

                double dev = r.getValue() - otherMean;
                numer.merge(dishId, s * dev, Double::sum);
                denom.merge(dishId, s, Double::sum);
            }
        }

        Map<Long, Double> preds = new HashMap<>(numer.size());
        for (Map.Entry<Long, Double> e : numer.entrySet()) {
            long dishId = e.getKey();
            double d = denom.getOrDefault(dishId, 0.0);
            if (d <= 0.0) continue;

            double p = targetMean + (e.getValue() / d);
            preds.put(dishId, clamp(p, MIN_RATING, MAX_RATING));
        }

        log.debug("Collaborative predictor produced {} dish scores for user {}", preds.size(), userId);
        return preds;
    }

    /**
     * Mean-centered cosine similarity:
     * sim = dot((a-meanA),(b-meanB)) / (||a-meanA|| * ||b-meanB||) over overlaps.
     * Uses only overlapping items (important).
     */
    private static Sim centeredCosine(Map<Long, Double> a, double meanA,
                                      Map<Long, Double> b, double meanB) {

        // iterate smaller map to find overlaps cheaply
        Map<Long, Double> small = a.size() <= b.size() ? a : b;
        Map<Long, Double> large = (small == a) ? b : a;

        double dot = 0.0, normA = 0.0, normB = 0.0;
        int overlap = 0;

        for (Map.Entry<Long, Double> e : small.entrySet()) {
            Double other = large.get(e.getKey());
            if (other == null) continue;

            // get aligned values depending on which map is "small"
            double va = (small == a) ? e.getValue() : other;
            double vb = (small == a) ? other : e.getValue();

            double da = va - meanA;
            double db = vb - meanB;

            dot += da * db;
            normA += da * da;
            normB += db * db;
            overlap++;
        }

        if (overlap == 0 || normA == 0.0 || normB == 0.0) {
            return new Sim(0.0, overlap);
        }
        return new Sim(dot / (Math.sqrt(normA) * Math.sqrt(normB)), overlap);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private record Sim(double value, int overlap) {}
}
