package com.exampleepam.restaurant.service.forecast;

import java.util.List;

/**
 * Utility methods for evaluating forecast accuracy.
 */
public final class ForecastEvaluator {
    private ForecastEvaluator() {}

    public static double mape(List<Double> actual, List<Double> forecast) {
        double sum = 0;
        int count = 0;
        int n = actual.size();
        for (int i = 0; i < n; i++) {
            double a = actual.get(i);
            if (a == 0) {
                continue;
            }
            sum += Math.abs((a - forecast.get(i)) / a);
            count++;
        }
        return count == 0 ? Double.NaN : 100.0 * sum / count;
    }

    public static double rmse(List<Double> actual, List<Double> forecast) {
        double sum = 0;
        int count = 0;
        int n = actual.size();
        for (int i = 0; i < n; i++) {
            double a = actual.get(i);
            if (a == 0) continue;
            double diff = a - forecast.get(i);
            sum += diff * diff;
            count++;
        }
        return count == 0 ? Double.NaN : Math.sqrt(sum / count);
    }

    /** Metrics container for cross-validation results. */
    public record Metrics(double mape, double rmse) {}

    /**
     * Performs a simple k-fold cross-validation where the series is split into
     * {@code k} contiguous folds. For each fold the model is trained on all
     * preceding data and evaluated on the next fold. The returned metrics are
     * averages across folds.
     */
    public static Metrics crossValidate(List<Integer> history, ForecastModel model, int k) {
        List<Integer> filtered = history.stream().filter(v -> v != 0).toList();
        int n = filtered.size();
        if (n < 2 || n < k + 1) {
            return new Metrics(Double.NaN, Double.NaN);
        }
        int foldSize = n / k;
        if (foldSize == 0) {
            return new Metrics(Double.NaN, Double.NaN);
        }
        double mapeSum = 0;
        double rmseSum = 0;
        int folds = 0;
        for (int i = foldSize; i <= n - foldSize; i += foldSize) {
            List<Integer> train = filtered.subList(0, i);
            List<Integer> test = filtered.subList(i, Math.min(i + foldSize, n));
            ForecastResult fr = model.forecast(train, test.size());
            List<Double> actual = new java.util.ArrayList<>();
            for (Integer t : test) actual.add(t.doubleValue());
            List<Double> preds = fr.getForecasts().subList(0, test.size());
            mapeSum += mape(actual, preds);
            rmseSum += rmse(actual, preds);
            folds++;
        }
        return folds == 0 ? new Metrics(Double.NaN, Double.NaN) : new Metrics(mapeSum / folds, rmseSum / folds);
    }
}
