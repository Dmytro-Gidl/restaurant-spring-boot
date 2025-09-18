package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Review;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Lightweight matrix factorization with SGD for implicit+explicit feedback.
 * - Uses user/item biases and global mean for better accuracy.
 * - Trains on: reviews (explicit rating) + orders (implicit = 1.0).
 * - Thread-safe for reads: training builds new factor maps and swaps them atomically.
 * - Hyperparameters are configurable via constructor.
 */
@Service
public class FactorizationService {

    // Model (published atomically after each train)
    private volatile Map<Long, double[]> userFactors = Collections.emptyMap();
    private volatile Map<Long, double[]> itemFactors = Collections.emptyMap();
    private volatile Map<Long, Double> userBias = Collections.emptyMap();
    private volatile Map<Long, Double> itemBias = Collections.emptyMap();
    private volatile double globalMean = 0.0;

    // Hyperparameters
    private final int factors;
    private final int epochs;
    private final double alpha;   // learning rate
    private final double lambda;  // L2 reg
    private final long seed;

    public FactorizationService() {
        // Sensible defaults; tune if needed
        this(8, 25, 0.02, 0.05, 0L);
    }

    public FactorizationService(int factors, int epochs, double alpha, double lambda, long seed) {
        if (factors <= 0) throw new IllegalArgumentException("factors must be > 0");
        if (epochs <= 0) throw new IllegalArgumentException("epochs must be > 0");
        if (alpha <= 0) throw new IllegalArgumentException("alpha must be > 0");
        if (lambda < 0) throw new IllegalArgumentException("lambda must be >= 0");
        this.factors = factors;
        this.epochs = epochs;
        this.alpha = alpha;
        this.lambda = lambda;
        this.seed = seed;
    }

    public boolean isReady() {
        return !userFactors.isEmpty() && !itemFactors.isEmpty();
    }

    /**
     * Trains the model on given reviews (explicit) and orders (implicit=1.0).
     * Safe for concurrent readers of predict(): swaps in new maps at the end.
     */
    public void train(List<Review> reviews, List<Order> orders) {
        Objects.requireNonNull(reviews, "reviews");
        Objects.requireNonNull(orders, "orders");

        // Local working state
        Map<Long, double[]> uFac = new HashMap<>();
        Map<Long, double[]> iFac = new HashMap<>();
        Map<Long, Double> uBias = new HashMap<>();
        Map<Long, Double> iBias = new HashMap<>();
        List<Interaction> interactions = new ArrayList<>(Math.max(16, reviews.size() + orders.size() * 3));

        Random rnd = new Random(seed);

        // Build interactions + initialize parameters
        double sumRatings = 0.0;
        int countRatings = 0;

        for (Review r : reviews) {
            long uid = r.getUser().getId();
            long did = r.getDish().getId();
            double rating = r.getRating(); // assume already normalized or raw star value

            uFac.computeIfAbsent(uid, k -> randomVector(rnd));
            iFac.computeIfAbsent(did, k -> randomVector(rnd));
            uBias.putIfAbsent(uid, 0.0);
            iBias.putIfAbsent(did, 0.0);

            interactions.add(new Interaction(uid, did, rating));
            sumRatings += rating;
            countRatings++;
        }

        for (Order o : orders) {
            long uid = o.getUser().getId();
            uFac.computeIfAbsent(uid, k -> randomVector(rnd));
            uBias.putIfAbsent(uid, 0.0);

            for (OrderItem it : o.getOrderItems()) {
                long did = it.getDish().getId();
                iFac.computeIfAbsent(did, k -> randomVector(rnd));
                iBias.putIfAbsent(did, 0.0);

                // Implicit positive signal
                interactions.add(new Interaction(uid, did, 1.0));
                sumRatings += 1.0;
                countRatings++;
            }
        }

        if (interactions.isEmpty()) {
            // Publish empty model
            this.userFactors = Collections.emptyMap();
            this.itemFactors = Collections.emptyMap();
            this.userBias = Collections.emptyMap();
            this.itemBias = Collections.emptyMap();
            this.globalMean = 0.0;
            return;
        }

        double gMean = sumRatings / countRatings;

        // Shuffle each epoch for better SGD
        for (int ep = 0; ep < epochs; ep++) {
            Collections.shuffle(interactions, rnd);
            for (Interaction in : interactions) {
                double[] uf = uFac.get(in.userId);
                double[] ifc = iFac.get(in.itemId);
                double ub = uBias.get(in.userId);
                double ib = iBias.get(in.itemId);

                double pred = gMean + ub + ib + dot(uf, ifc);
                double err = in.rating - pred;

                // Update biases
                double newUb = ub + alpha * (err - lambda * ub);
                double newIb = ib + alpha * (err - lambda * ib);
                uBias.put(in.userId, newUb);
                iBias.put(in.itemId, newIb);

                // Update factors
                for (int f = 0; f < factors; f++) {
                    double ufOld = uf[f];
                    double ifOld = ifc[f];

                    uf[f] += alpha * (err * ifOld - lambda * ufOld);
                    ifc[f] += alpha * (err * ufOld - lambda * ifOld);
                }
            }
        }

        // Publish atomically for readers
        this.userFactors = Collections.unmodifiableMap(uFac);
        this.itemFactors = Collections.unmodifiableMap(iFac);
        this.userBias = Collections.unmodifiableMap(uBias);
        this.itemBias = Collections.unmodifiableMap(iBias);
        this.globalMean = gMean;
    }

    /**
     * Predicts preference score. If user/item unseen, backs off to biases/mean.
     * Range depends on your input ratings; consider downstream clipping if needed.
     */
    public double predict(long userId, long dishId) {
        double[] uf = userFactors.get(userId);
        double[] ifc = itemFactors.get(dishId);
        Double ub = userBias.get(userId);
        Double ib = itemBias.get(dishId);

        double pred = globalMean;
        if (ub != null) pred += ub;
        if (ib != null) pred += ib;
        if (uf != null && ifc != null) pred += dot(uf, ifc);

        return pred;
    }

    /**
     * Quick sanity check: RMSE on provided reviews only.
     * Useful for monitoring training stability/regressions.
     */
    public double rmseOnReviews(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty() || !isReady()) return Double.NaN;
        double se = 0.0;
        int n = 0;
        for (Review r : reviews) {
            long uid = r.getUser().getId();
            long did = r.getDish().getId();
            double err = r.getRating() - predict(uid, did);
            se += err * err;
            n++;
        }
        return n == 0 ? Double.NaN : Math.sqrt(se / n);
    }

    // --- internals ---

    private static final class Interaction {
        final long userId;
        final long itemId;
        final double rating;
        Interaction(long u, long i, double r) { this.userId = u; this.itemId = i; this.rating = r; }
    }

    private double[] randomVector(Random rnd) {
        double[] v = new double[factors];
        for (int i = 0; i < factors; i++) v[i] = (rnd.nextDouble() - 0.5) * 0.02; // small values around 0
        return v;
    }

    private static double dot(double[] a, double[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }
}
