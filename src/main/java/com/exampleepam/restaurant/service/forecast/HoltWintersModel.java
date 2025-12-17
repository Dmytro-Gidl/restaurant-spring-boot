package com.exampleepam.restaurant.service.forecast;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Triple exponential smoothing with additive seasonality. Parameters are
 * selected via grid search to minimise RMSE on a hold-out slice.
 */
@Component
public class HoltWintersModel implements ForecastModel {

    private final int period;

    public HoltWintersModel(@Value("${forecast.period:12}") int period) {
        this.period = period;
    }

    @Override
    public String getName() {
        return "holt";
    }

    @Override
    public ForecastResult forecast(List<Integer> history, int periods) {
        if (history == null || history.isEmpty() || periods <= 0) {
            return new ForecastResult(List.of(), 0d, 0d, 0d, 0d, 0d, List.of(), List.of());
        }

        int n = history.size();
        if (n < 2 * period) {
            double last = history.get(n - 1);
            List<Double> future = new ArrayList<>();
            for (int i = 0; i < periods; i++) {
                future.add(last);
            }
            return new ForecastResult(future, 0d, 0d, 0d, 0d, 0d, List.of(), List.of());
        }

        int holdout = period;
        List<Integer> train = history.subList(0, n - holdout);
        List<Integer> test = history.subList(n - holdout, n);

        double bestRmse = Double.POSITIVE_INFINITY;
        double bestA = 0.2, bestB = 0.1, bestC = 0.1;

        for (double a = 0.1; a <= 1.0; a += 0.1) {
            for (double b = 0.1; b <= 1.0; b += 0.1) {
                for (double g = 0.1; g <= 1.0; g += 0.1) {
                    List<Double> fit = smooth(train, holdout, a, b, g);
                    List<Double> validation = fit.subList(train.size(), train.size() + holdout);
                    List<Double> testD = new ArrayList<>(holdout);
                    for (int v : test) testD.add((double) v);
                    double rmse = ForecastEvaluator.rmse(testD, validation);
                    if (rmse < bestRmse) {
                        bestRmse = rmse;
                        bestA = a;
                        bestB = b;
                        bestC = g;
                    }
                }
            }
        }

        List<Double> testD = new ArrayList<>(holdout);
        for (int v : test) testD.add((double) v);
        List<Double> validation = smooth(train, holdout, bestA, bestB, bestC)
                .subList(train.size(), train.size() + holdout);
        double mape = ForecastEvaluator.mape(testD, validation);

        List<Double> fullFit = smooth(history, periods, bestA, bestB, bestC);
        List<Double> future = fullFit.subList(history.size(), history.size() + periods);

        double interval = 1.96 * bestRmse;
        List<Double> lower = new ArrayList<>(periods);
        List<Double> upper = new ArrayList<>(periods);
        for (double v : future) {
            lower.add(Math.max(0, v - interval));
            upper.add(Math.max(0, v + interval));
        }

        return new ForecastResult(future, bestA, bestB, bestC, mape, bestRmse, lower, upper);
    }

    private List<Double> smooth(List<Integer> data, int forecastPeriods, double a, double b, double g) {
        int n = data.size();
        int m = period;

        if (n < 2 * m) {
            double last = data.get(n - 1);
            List<Double> out = new ArrayList<>(n + forecastPeriods);
            for (int value : data) {
                out.add((double) value);
            }
            for (int k = 0; k < forecastPeriods; k++) {
                out.add(last);
            }
            return out;
        }

        double level = avg(data, 0, m);
        double level2 = avg(data, m, 2 * m);
        double trend = (level2 - level) / m;

        double[] season = new double[n];
        for (int i = 0; i < m; i++) {
            season[i] = data.get(i) - level;
        }

        List<Double> out = new ArrayList<>(n + forecastPeriods);

        for (int t = 0; t < m; t++) {
            out.add((double) data.get(t));
        }

        for (int t = m; t < n; t++) {
            double yt = data.get(t);
            double lastSeason = season[t - m];

            double yhat = level + trend + lastSeason;
            out.add(yhat);

            double newLevel = a * (yt - lastSeason) + (1 - a) * (level + trend);
            double newTrend = b * (newLevel - level) + (1 - b) * trend;
            double newSeason = g * (yt - newLevel) + (1 - g) * lastSeason;

            level = newLevel;
            trend = newTrend;
            season[t] = newSeason;
        }

        for (int k = 1; k <= forecastPeriods; k++) {
            double s = season[n - m + (k - 1) % m];
            out.add(level + k * trend + s);
        }

        return out;
    }

    private double avg(List<Integer> data, int from, int toExclusive) {
        double sum = 0;
        for (int i = from; i < toExclusive; i++) {
            sum += data.get(i);
        }
        return sum / (toExclusive - from);
    }
}

