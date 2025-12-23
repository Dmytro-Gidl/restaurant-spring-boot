package com.exampleepam.restaurant.service.forecast;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal AR(1) == ARIMA(1,0,0):
 *  y_t = c + phi * y_{t-1} + e_t
 *
 * OLS phi/intercept, recursive forecast, and simple 95% CI for stationary AR(1).
 */
@Slf4j
@Component
public class ArimaModel implements ForecastModel {

    private static final double PHI_CLAMP = 0.99; // keep |phi| < 1 for stability
    private static final double Z95 = 1.96;       // 95% CI z-score
    private static final double EPS = 1e-12;

    @Override
    public String getName() {
        return "arima";
    }

    @Override
    public ForecastResult forecast(List<Integer> history, int periods) {
        log.debug("ARIMA forecast requested with size={} periods={}",
                history == null ? null : history.size(), periods);

        if (periods <= 0) {
            return emptyResult(0);
        }

        double[] y = toSeries(history);
        if (y.length == 0) {
            return emptyResult(periods);
        }
        if (y.length == 1) {
            return singlePointResult(y[0], periods);
        }

        Fit fit = olsFitWithIntercept(y);
        Diagnostics diag = diagnostics(y, fit);

        // obs = n-1, params = 2 (intercept + phi)
        int obs = y.length - 1;
        int dof = Math.max(1, obs - 2);
        double sigma2 = diag.sse / dof;

        ForecastPath path = forecastWithCi(y[y.length - 1], fit, sigma2, periods);

        log.debug("ARIMA phi={} intercept={} rmse={} mape={}", fit.phi, fit.intercept, diag.rmse, diag.mape);
        return new ForecastResult(
                path.forecasts,
                fit.phi,
                0,
                fit.intercept,
                diag.mape,
                diag.rmse,
                path.lower,
                path.upper
        );
    }

    private static ForecastResult emptyResult(int periods) {
        List<Double> zeros = constantList(periods, 0d);
        return new ForecastResult(zeros, 0, 0, 0, 0, 0, zeros, zeros);
    }

    private static ForecastResult singlePointResult(double val, int periods) {
        List<Double> fc = constantList(periods, val);
        return new ForecastResult(fc, 1, 0, 0, 0, 0, fc, fc);
    }

    /**
     * "Null-safe": maps null elements to 0.0. If you'd rather reject, throw instead.
     */
    private static double[] toSeries(List<Integer> history) {
        if (history == null || history.isEmpty()) return new double[0];

        double[] y = new double[history.size()];
        boolean sawNull = false;

        for (int i = 0; i < history.size(); i++) {
            Integer v = history.get(i);
            if (v == null) {
                y[i] = 0.0;
                sawNull = true;
            } else {
                y[i] = v.doubleValue();
            }
        }

        if (sawNull) {
            // keep it quiet-ish: one line, no spam
            // (remove if you prefer strict data quality)
            // log.debug(...) can't be used here (static), so caller logs size only.
        }
        return y;
    }

    private static Fit olsFitWithIntercept(double[] y) {
        int n = y.length;

        // x = y[t-1], y = y[t]
        double sumX = 0.0, sumY = 0.0;
        for (int t = 1; t < n; t++) {
            sumX += y[t - 1];
            sumY += y[t];
        }

        int obs = n - 1;
        double meanX = sumX / obs;
        double meanY = sumY / obs;

        double num = 0.0, den = 0.0;
        for (int t = 1; t < n; t++) {
            double x = y[t - 1] - meanX;
            double yt = y[t] - meanY;
            num += x * yt;
            den += x * x;
        }

        double phi = (Math.abs(den) < EPS) ? 0.0 : (num / den);
        phi = clamp(phi, -PHI_CLAMP, PHI_CLAMP);

        double intercept = meanY - phi * meanX;
        return new Fit(phi, intercept);
    }

    private static Diagnostics diagnostics(double[] y, Fit fit) {
        int n = y.length;

        double sse = 0.0;
        // If you want to keep using ForecastEvaluator for consistency, keep lists.
        // Otherwise compute rmse/mape here to avoid allocations.
        List<Double> fitted = new ArrayList<>(n - 1);
        List<Double> actual = new ArrayList<>(n - 1);

        for (int t = 1; t < n; t++) {
            double f = fit.intercept + fit.phi * y[t - 1];
            double a = y[t];
            fitted.add(f);
            actual.add(a);
            double e = a - f;
            sse += e * e;
        }

        double rmse = ForecastEvaluator.rmse(actual, fitted);
        double mape = ForecastEvaluator.mape(actual, fitted);
        return new Diagnostics(sse, rmse, mape);
    }

    private static ForecastPath forecastWithCi(double lastValue, Fit fit, double sigma2, int periods) {
        List<Double> forecasts = new ArrayList<>(periods);
        List<Double> lower = new ArrayList<>(periods);
        List<Double> upper = new ArrayList<>(periods);

        double phi = fit.phi;
        double intercept = fit.intercept;

        double denom = 1.0 - (phi * phi);
        double last = lastValue;

        // iterative phi^(2h) to avoid Math.pow in loop
        double phi2 = phi * phi;
        double phi2h = phi2; // for h=1

        for (int h = 1; h <= periods; h++) {
            last = intercept + phi * last;
            forecasts.add(last);

            double varH;
            if (Math.abs(denom) < EPS) {
                // near unit root: linear growth fallback
                varH = sigma2 * h;
            } else {
                // var_h = sigma^2 * (1 - phi^(2h)) / (1 - phi^2)
                varH = sigma2 * (1.0 - phi2h) / denom;
            }

            double se = Math.sqrt(Math.max(0.0, varH));
            lower.add(last - Z95 * se);
            upper.add(last + Z95 * se);

            // update for next h
            phi2h *= phi2;
        }

        return new ForecastPath(forecasts, lower, upper);
    }

    private static List<Double> constantList(int n, double v) {
        if (n <= 0) return Collections.emptyList();
        List<Double> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(v);
        return out;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private record Fit(double phi, double intercept) {}
    private record Diagnostics(double sse, double rmse, double mape) {}
    private record ForecastPath(List<Double> forecasts, List<Double> lower, List<Double> upper) {}
}
