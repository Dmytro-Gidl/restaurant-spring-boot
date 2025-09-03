package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.entity.Review;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Very small matrix factorisation component using stochastic gradient descent.
 * It is intentionally lightweight to avoid extra dependencies while still
 * demonstrating the concept of latent factor recommenders.
 */
@Service
public class FactorizationService {
    private final Map<Long, double[]> userFactors = new HashMap<>();
    private final Map<Long, double[]> itemFactors = new HashMap<>();
    private final int factors = 3;

    public boolean isReady() { return !userFactors.isEmpty(); }

    public void train(List<Review> reviews, List<Order> orders) {
        userFactors.clear();
        itemFactors.clear();
        Random rnd = new Random(0);
        class Interaction { long u; long d; double r; Interaction(long u,long d,double r){this.u=u;this.d=d;this.r=r;} }
        List<Interaction> interactions = new java.util.ArrayList<>();
        for (Review r : reviews) {
            userFactors.computeIfAbsent(r.getUser().getId(), k -> randomVector(rnd));
            itemFactors.computeIfAbsent(r.getDish().getId(), k -> randomVector(rnd));
            interactions.add(new Interaction(r.getUser().getId(), r.getDish().getId(), r.getRating()));
        }
        for (Order o : orders) {
            long uid = o.getUser().getId();
            for (OrderItem it : o.getOrderItems()) {
                long did = it.getDish().getId();
                userFactors.computeIfAbsent(uid, k -> randomVector(rnd));
                itemFactors.computeIfAbsent(did, k -> randomVector(rnd));
                interactions.add(new Interaction(uid, did, 1.0));
            }
        }
        double alpha = 0.01;
        double lambda = 0.1;
        for (int it = 0; it < 20; it++) {
            for (Interaction in : interactions) {
                double[] u = userFactors.get(in.u);
                double[] i = itemFactors.get(in.d);
                double err = in.r - dot(u, i);
                for (int f = 0; f < factors; f++) {
                    double uf = u[f];
                    double ifac = i[f];
                    u[f] += alpha * (err * ifac - lambda * uf);
                    i[f] += alpha * (err * uf - lambda * ifac);
                }
            }
        }
    }

    public double predict(long userId, long dishId) {
        double[] u = userFactors.get(userId);
        double[] i = itemFactors.get(dishId);
        if (u == null || i == null) return 0;
        return dot(u, i);
    }

    private double[] randomVector(Random rnd) {
        double[] v = new double[factors];
        for (int i = 0; i < factors; i++) {
            v[i] = rnd.nextDouble() * 0.1;
        }
        return v;
    }

    private double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < factors; i++) s += a[i] * b[i];
        return s;
    }
}
