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

    private static final class HWState {
        private double level;
        private double trend;
        private double[] season;
        private int n;
    }

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
        if (period <= 0) {
            return naiveLast(history, periods);
        }

        if (n < period + 2) {
            return naiveLast(history, periods);
        }

        int holdout = Math.min(period, n - period);
        if (holdout <= 0) {
            return naiveLast(history, periods);
        }
        List<Integer> train = history.subList(0, n - holdout);
        List<Integer> test = history.subList(n - holdout, n);
        if (train.size() < period) {
            return naiveLast(history, periods);
        }

        double bestRmse = Double.POSITIVE_INFINITY;
        double bestA = 0.3, bestB = 0.1, bestC = 0.1;
        boolean tuneable = train.size() >= 2 * period;
        List<Double> bestValidation = null;

        List<Double> testD = new ArrayList<>(holdout);
        for (int v : test) testD.add((double) v);

        if (tuneable) {
            for (int ia = 1; ia <= 10; ia++) {
                double a = ia / 10.0;
                for (int ib = 1; ib <= 10; ib++) {
                    double b = ib / 10.0;
                    for (int ig = 1; ig <= 10; ig++) {
                        double g = ig / 10.0;
                        HWState trainState = fitAdditive(train, period, a, b, g);
                        List<Double> validation = clampNonNegative(forecastAdditive(trainState, period, holdout));
                        double rmse = ForecastEvaluator.rmse(testD, validation);
                        if (rmse < bestRmse) {
                            bestRmse = rmse;
                            bestA = a;
                            bestB = b;
                            bestC = g;
                            bestValidation = validation;
                        }
                    }
                }
            }
        } else {
            HWState trainState = fitAdditive(train, period, bestA, bestB, bestC);
            bestValidation = clampNonNegative(forecastAdditive(trainState, period, holdout));
            bestRmse = ForecastEvaluator.rmse(testD, bestValidation);
        }

        if (bestValidation == null) {
            HWState trainState = fitAdditive(train, period, bestA, bestB, bestC);
            bestValidation = clampNonNegative(forecastAdditive(trainState, period, holdout));
        }
        double mape = ForecastEvaluator.mape(testD, bestValidation);

        HWState fullState = fitAdditive(history, period, bestA, bestB, bestC);
        List<Double> future = clampNonNegative(forecastAdditive(fullState, period, periods));

        double intervalRmse = inSampleRmse(history, period, bestA, bestB, bestC);
        if (Double.isNaN(intervalRmse)) {
            intervalRmse = bestRmse;
        }

        double intervalBase = 1.96 * intervalRmse;
        List<Double> lower = new ArrayList<>(periods);
        List<Double> upper = new ArrayList<>(periods);
        for (int i = 0; i < periods; i++) {
            double width = intervalBase * Math.sqrt(i + 1);
            double v = future.get(i);
            lower.add(Math.max(0, v - width));
            upper.add(Math.max(0, v + width));
        }

        return new ForecastResult(future, bestA, bestB, bestC, mape, bestRmse, lower, upper);
    }

    private HWState fitAdditive(List<Integer> data, int period, double a, double b, double g) {
        int n = data.size();
        double firstSeasonAvg = 0.0;
        for (int i = 0; i < period; i++) {
            firstSeasonAvg += data.get(i);
        }
        firstSeasonAvg /= period;

        double trend;
        if (n >= 2 * period) {
            double secondSeasonAvg = 0.0;
            for (int i = period; i < 2 * period; i++) {
                secondSeasonAvg += data.get(i);
            }
            secondSeasonAvg /= period;
            trend = (secondSeasonAvg - firstSeasonAvg) / period;
        } else {
            trend = n > 1 ? data.get(1) - data.get(0) : 0.0;
        }

        double[] season = new double[period];
        for (int i = 0; i < period; i++) {
            season[i] = data.get(i) - firstSeasonAvg;
        }

        double level = firstSeasonAvg;

        for (int t = 0; t < n; t++) {
            int si = t % period;
            double y = data.get(t);
            double s = season[si];

            double prevLevel = level;

            level = a * (y - s) + (1 - a) * (level + trend);
            trend = b * (level - prevLevel) + (1 - b) * trend;
            season[si] = g * (y - level) + (1 - g) * s;
        }

        HWState st = new HWState();
        st.level = level;
        st.trend = trend;
        st.season = season;
        st.n = n;
        return st;
    }

    private double inSampleRmse(List<Integer> data, int period, double a, double b, double g) {
        int n = data.size();
        double firstSeasonAvg = 0.0;
        for (int i = 0; i < period; i++) {
            firstSeasonAvg += data.get(i);
        }
        firstSeasonAvg /= period;

        double trend;
        if (n >= 2 * period) {
            double secondSeasonAvg = 0.0;
            for (int i = period; i < 2 * period; i++) {
                secondSeasonAvg += data.get(i);
            }
            secondSeasonAvg /= period;
            trend = (secondSeasonAvg - firstSeasonAvg) / period;
        } else {
            trend = n > 1 ? data.get(1) - data.get(0) : 0.0;
        }

        double[] season = new double[period];
        for (int i = 0; i < period; i++) {
            season[i] = data.get(i) - firstSeasonAvg;
        }

        double level = firstSeasonAvg;
        List<Double> actual = new ArrayList<>(n - period);
        List<Double> forecast = new ArrayList<>(n - period);
        for (int t = 0; t < n; t++) {
            int si = t % period;
            double s = season[si];
            if (t >= period) {
                actual.add((double) data.get(t));
                forecast.add(Math.max(0, level + trend + s));
            }

            double y = data.get(t);
            double prevLevel = level;
            level = a * (y - s) + (1 - a) * (level + trend);
            trend = b * (level - prevLevel) + (1 - b) * trend;
            season[si] = g * (y - level) + (1 - g) * s;
        }
        return ForecastEvaluator.rmse(actual, forecast);
    }

    private List<Double> forecastAdditive(HWState st, int period, int horizon) {
        List<Double> out = new ArrayList<>(horizon);
        for (int h = 1; h <= horizon; h++) {
            int si = (st.n + h - 1) % period;
            out.add(st.level + h * st.trend + st.season[si]);
        }
        return out;
    }

    private List<Double> clampNonNegative(List<Double> values) {
        List<Double> out = new ArrayList<>(values.size());
        for (double v : values) {
            out.add(Math.max(0, v));
        }
        return out;
    }

    private ForecastResult naiveLast(List<Integer> history, int periods) {
        int n = history.size();
        double last = history.get(n - 1);
        List<Double> future = new ArrayList<>();
        for (int i = 0; i < periods; i++) {
            future.add(last);
        }
        return new ForecastResult(future, 0d, 0d, 0d, 0d, 0d, List.of(), List.of());
    }
}
