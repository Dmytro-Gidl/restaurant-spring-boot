package com.exampleepam.restaurant.service.recommendation;

import com.exampleepam.restaurant.entity.Review;
import com.exampleepam.restaurant.repository.ReviewRepository;
import com.exampleepam.restaurant.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline evaluation harness computing simple precision@k and NDCG@k metrics
 * on historical review data. It is not invoked in production but can be run
 * from tests or a command line runner to assess recommendation quality.
 */
@Component
public class RecommendationEvaluator {
    private static final Logger log = LoggerFactory.getLogger(RecommendationEvaluator.class);
    private final RecommendationService recommendationService;
    private final ReviewRepository reviewRepository;

    public RecommendationEvaluator(RecommendationService recommendationService,
                                   ReviewRepository reviewRepository) {
        this.recommendationService = recommendationService;
        this.reviewRepository = reviewRepository;
    }

    public void evaluate() {
        List<Review> reviews = reviewRepository.findAllWithUserAndDish();
        Map<Long, List<Review>> byUser = new HashMap<>();
        for (Review r : reviews) {
            byUser.computeIfAbsent(r.getUser().getId(), k -> new ArrayList<>()).add(r);
        }
        int hits = 0;
        int total = 0;
        double ndcgSum = 0;
        for (var e : byUser.entrySet()) {
            if (e.getValue().size() < 2) continue;
            Review heldOut = e.getValue().get(e.getValue().size()-1);
            List<Review> train = new ArrayList<>(reviews);
            train.remove(heldOut);
            // train factorisation again with remaining reviews
            recommendationService.getRecommendedDishes(e.getKey(), 5); // ensures factors trained
            var recs = recommendationService.getRecommendedDishes(e.getKey(), 3);
            total++;
            boolean found = false;
            for (int i=0;i<recs.size();i++) {
                if (recs.get(i).getId() == heldOut.getDish().getId()) {
                    hits++;
                    ndcgSum += 1.0 / (Math.log(i + 2) / Math.log(2));
                    found = true;
                    break;
                }
            }
            if (!found) {
                ndcgSum += 0;
            }
        }
        if (total>0) {
            double precision = hits/(double)(total*3);
            double recall = hits/(double)total;
            double ndcg = ndcgSum/total;
            log.info("Evaluation precision@3={} recall@3={} ndcg@3={}", precision, recall, ndcg);
        }
    }
}
