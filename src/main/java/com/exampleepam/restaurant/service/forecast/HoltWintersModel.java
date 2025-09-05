package com.exampleepam.restaurant.service.forecast;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
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
        if (history.isEmpty()) {
            return new ForecastResult(Collections.emptyList(), 0d, 0d, 0d, 0d, 0d,
                    Collections.emptyList(), Collections.emptyList());
        }
        int n = history.size();
        if (n <= period) {
            double last = history.get(n - 1);
            List<Double> future = new ArrayList<>();
            for (int i = 0; i < periods; i++) {
                future.add(last);
            }
            return new ForecastResult(future, 0d, 0d, 0d, 0d, 0d,
                    Collections.emptyList(), Collections.emptyList());
        }
        int split = Math.max(period * 2, period + 1);
        split = Math.min(split, n);
        if (split - period <= 0) {
            double last = history.get(n - 1);
            List<Double> future = new ArrayList<>();
            for (int i = 0; i < periods; i++) {
                future.add(last);
            }
            return new ForecastResult(future, 0d, 0d, 0d, 0d, 0d,
                    Collections.emptyList(), Collections.emptyList());
        }
        List<Integer> train = history.subList(0, split - period);
        List<Integer> test = history.subList(split - period, split);

        // If the training slice is shorter than a full seasonal cycle, fall back to
        // a naive forecast instead of attempting Holt-Winters smoothing which
        // would access missing indices.
        if (train.size() < period || train.size() < 2) {
            double last = history.get(n - 1);
            List<Double> future = new ArrayList<>();
            for (int i = 0; i < periods; i++) {
                future.add(last);
            }
            return new ForecastResult(future, 0d, 0d, 0d, 0d, 0d,
                    Collections.emptyList(), Collections.emptyList());
        }

        double bestRmse = Double.MAX_VALUE;
        double bestA = 0.2, bestB = 0.1, bestC = 0.1;
        List<Double> bestForecast = null;

        for (double a = 0.1; a <= 1.0; a += 0.1) {
            for (double b = 0.1; b <= 1.0; b += 0.1) {
                for (double g = 0.1; g <= 1.0; g += 0.1) {
                    List<Double> fit = smooth(train, periods + period, a, b, g);
                    List<Double> validation = fit.subList(periods, periods + period);
                    List<Double> testD = new ArrayList<>();
                    for (int v : test) testD.add((double) v);
                    double rmse = ForecastEvaluator.rmse(testD, validation);
                    if (rmse < bestRmse) {
                        bestRmse = rmse;
                        bestA = a; bestB = b; bestC = g;
                        bestForecast = fit;
                    }
                }
            }
        }

        // compute accuracy on test slice
        List<Double> testD = new ArrayList<>();
        for (int v : test) testD.add((double) v);
        List<Double> validation = bestForecast.subList(periods, periods + period);
        double mape = ForecastEvaluator.mape(testD, validation);

        List<Double> future = bestForecast.subList(0, periods);
        double interval = 1.96 * bestRmse;
        List<Double> lower = new ArrayList<>();
        List<Double> upper = new ArrayList<>();
        for (double v : future) {
            lower.add(v - interval);
            upper.add(v + interval);
        }
        return new ForecastResult(future, bestA, bestB, bestC, mape, bestRmse, lower, upper);
    }

    private List<Double> smooth(List<Integer> data, int periods, double a, double b, double g) {
        int n = data.size();
        double[] level = new double[n + periods];
        double[] trend = new double[n + periods];
        double[] season = new double[n + periods];
        List<Double> result = new ArrayList<>();
        // initialise
        level[0] = data.get(0);
        trend[0] = (n > 1) ? data.get(1) - data.get(0) : 0;
        int initLen = Math.min(period, n);
        for (int i = 0; i < initLen; i++) {
            season[i] = data.get(i);
        }
        for (int i = 0; i < n + periods; i++) {
            if (i < n) {
                double val = data.get(i);
                double lastSeason = season[(i >= period) ? i - period : i];
                level[i] = a * (val - lastSeason) + (1 - a) * (level[i > 0 ? i - 1 : 0] + trend[i > 0 ? i - 1 : 0]);
                trend[i] = b * (level[i] - level[i > 0 ? i - 1 : 0]) + (1 - b) * trend[i > 0 ? i - 1 : 0];
                season[i] = g * (val - level[i]) + (1 - g) * lastSeason;
                result.add(level[i] + trend[i] + season[i]);
            } else {
                int idx = i;
                double lastSeason = season[idx - period];
                result.add((level[idx - 1] + trend[idx - 1]) + lastSeason);
                level[idx] = level[idx - 1] + trend[idx - 1];
                trend[idx] = trend[idx - 1];
                season[idx] = season[idx - period];
            }
        }
        return result;
    }
}

