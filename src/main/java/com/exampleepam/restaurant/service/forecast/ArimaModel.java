package com.exampleepam.restaurant.service.forecast;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal ARIMA(1,0,0) implementation using a single autoregressive term.
 * The AR coefficient is estimated by least squares and forecasts are
 * generated recursively. This lightweight model allows comparison with
 * Holt-Winters without pulling additional dependencies.
 */
@Slf4j
@Component
public class ArimaModel implements ForecastModel {

    @Override
    public String getName() {
        return "arima";
    }

    @Override
    public ForecastResult forecast(List<Integer> history, int periods) {
        log.debug("ARIMA forecast requested with history={} periods={}", history, periods);
        if (history.isEmpty()) {
            log.debug("No history provided; returning zeros");
            return new ForecastResult(Collections.nCopies(periods, 0d), 0, 0, 0, 0, 0,
                    Collections.nCopies(periods, 0d), Collections.nCopies(periods, 0d));
        }
        if (history.size() == 1) {
            double val = history.get(0);
            log.debug("Single data point {} – repeating for naive forecast", val);
            List<Double> forecasts = new ArrayList<>(Collections.nCopies(periods, val));
            return new ForecastResult(forecasts, 1, 0, 0, 0, 0,
                    Collections.nCopies(periods, 0d), Collections.nCopies(periods, 0d));
        }
        int n = history.size();
        double num = 0.0;
        double den = 0.0;
        for (int t = 1; t < n; t++) {
            double yt = history.get(t);
            double yt1 = history.get(t - 1);
            num += yt * yt1;
            den += yt1 * yt1;
        }
        double phi = den == 0 ? 0 : num / den;
        List<Double> forecasts = new ArrayList<>();
        double last = history.get(n - 1);
        for (int i = 0; i < periods; i++) {
            last = phi * last;
            forecasts.add(last);
        }
        log.debug("ARIMA phi={} last={} forecasts={}", phi, history.get(n - 1), forecasts);
        // diagnostics on training data
        List<Double> actual = new ArrayList<>();
        List<Double> fitted = new ArrayList<>();
        for (int t = 1; t < n; t++) {
            double fit = phi * history.get(t - 1);
            fitted.add(fit);
            actual.add((double) history.get(t));
        }
        double mape = ForecastEvaluator.mape(actual, fitted);
        double rmse = ForecastEvaluator.rmse(actual, fitted);
        return new ForecastResult(forecasts, phi, 0, 0, mape, rmse,
                Collections.nCopies(periods, 0d), Collections.nCopies(periods, 0d));
    }
}
