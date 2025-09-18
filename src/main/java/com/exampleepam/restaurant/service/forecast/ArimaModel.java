package com.exampleepam.restaurant.service.forecast;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal AR(1) (ARIMA(1,0,0)) with OLS phi, recursive forecast, and simple 95% CIs.
 * Stable, null-safe, and evaluated on t=1..n-1 (matches AutoArima fairness window).
 */
@Slf4j
@Component
public class ArimaModel implements ForecastModel {

    private static final double PHI_CLAMP = 0.99; // keep |phi| < 1 for stability
    private static final double Z95 = 1.96;       // 95% CI z-score

    @Override
    public String getName() {
        return "arima";
    }

    @Override
    public ForecastResult forecast(List<Integer> history, int periods) {
        log.debug("ARIMA forecast requested with history={} periods={}", history, periods);

        if (history == null || history.isEmpty() || periods <= 0) {
            int len = Math.max(periods, 0);
            return new ForecastResult(
                    Collections.nCopies(len, 0d),
                    0, 0, 0,
                    0, 0,
                    Collections.nCopies(len, 0d),
                    Collections.nCopies(len, 0d)
            );
        }

        final int n = history.size();

        // Single-point fallback: naive repeat (and same for CIs)
        if (n == 1) {
            double val = history.get(0);
            List<Double> fc = new ArrayList<>(Collections.nCopies(periods, val));
            return new ForecastResult(
                    fc, 1, 0, 0,
                    0, 0,
                    new ArrayList<>(Collections.nCopies(periods, val)),
                    new ArrayList<>(Collections.nCopies(periods, val))
            );
        }

        // OLS phi from y_t = phi * y_{t-1}
        double num = 0.0, den = 0.0;
        for (int t = 1; t < n; t++) {
            double yt = history.get(t);
            double yt1 = history.get(t - 1);
            num += yt * yt1;
            den += yt1 * yt1;
        }
        double phi = (den == 0.0) ? 0.0 : (num / den);
        if (phi > PHI_CLAMP) phi = PHI_CLAMP;
        if (phi < -PHI_CLAMP) phi = -PHI_CLAMP;

        // Fit/diagnostics on t=1..n-1
        List<Double> fitted = new ArrayList<>(n - 1);
        List<Double> actual = new ArrayList<>(n - 1);
        double sse = 0.0;
        for (int t = 1; t < n; t++) {
            double fit = phi * history.get(t - 1);
            double act = history.get(t);
            fitted.add(fit);
            actual.add(act);
            double e = act - fit;
            sse += e * e;
        }
        double rmse = ForecastEvaluator.rmse(actual, fitted);
        double mape = ForecastEvaluator.mape(actual, fitted);

        // Innovation variance estimate
        double sigma2 = sse / (n - 1);

        // Forecasts + simple AR(1) CI: var_h = sigma^2 * (1 - phi^(2h)) / (1 - phi^2)
        List<Double> forecasts = new ArrayList<>(periods);
        List<Double> lower = new ArrayList<>(periods);
        List<Double> upper = new ArrayList<>(periods);

        double last = history.get(n - 1);
        double denom = 1.0 - (phi * phi);
        for (int h = 1; h <= periods; h++) {
            last = phi * last;
            forecasts.add(last);

            double varH;
            if (Math.abs(denom) < 1e-12) {
                // near-unit-root fallback: linear growth
                varH = sigma2 * h;
            } else {
                double phi2h = Math.pow(phi, 2 * h);
                varH = sigma2 * (1.0 - phi2h) / denom;
            }
            double se = Math.sqrt(Math.max(0.0, varH));
            lower.add(last - Z95 * se);
            upper.add(last + Z95 * se);
        }

        log.debug("ARIMA phi={} rmse={} mape={}", phi, rmse, mape);
        return new ForecastResult(forecasts, phi, 0, 0, mape, rmse, lower, upper);
    }
}
