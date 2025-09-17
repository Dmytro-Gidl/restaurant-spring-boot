package com.exampleepam.restaurant.service.forecast;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Auto selector: compares mean (ARIMA(0,0,0)) vs AR(1) on the SAME window (t=1..n-1),
 * returns the lower-RMSE model. Edge-case safe and Spring-friendly.
 */
@Component
public class AutoArimaModel implements ForecastModel {

    private final ArimaModel ar1;

    public AutoArimaModel(ArimaModel ar1) {
        this.ar1 = ar1;
    }

    @Override
    public String getName() {
        return "auto-arima";
    }

    @Override
    public ForecastResult forecast(List<Integer> history, int periods) {
        if (history == null || history.isEmpty() || periods <= 0) {
            return new ForecastResult(
                    Collections.nCopies(Math.max(periods, 0), 0d),
                    0, 0, 0,
                    0, 0,
                    Collections.nCopies(Math.max(periods, 0), 0d),
                    Collections.nCopies(Math.max(periods, 0), 0d)
            );
        }

        // Single point → both models reduce to naive repeat
        if (history.size() == 1) {
            double v = history.get(0);
            List<Double> fc = new ArrayList<>(Collections.nCopies(periods, v));
            return new ForecastResult(
                    fc, 0, 0, 0,
                    0, 0,
                    new ArrayList<>(Collections.nCopies(periods, v)),
                    new ArrayList<>(Collections.nCopies(periods, v))
            );
        }

        final int n = history.size();
        double mean = history.stream().mapToDouble(Integer::doubleValue).average().orElse(0);

        // Mean diagnostics on SAME window as AR(1): t = 1..n-1
        List<Double> meanActual = new ArrayList<>(n - 1);
        List<Double> meanFitted = new ArrayList<>(n - 1);
        for (int t = 1; t < n; t++) {
            meanActual.add((double) history.get(t));
            meanFitted.add(mean);
        }
        double meanMape = ForecastEvaluator.mape(meanActual, meanFitted);
        double meanRmse = ForecastEvaluator.rmse(meanActual, meanFitted);

        List<Double> meanFc = new ArrayList<>(Collections.nCopies(periods, mean));
        ForecastResult meanResult = new ForecastResult(
                meanFc, 0, 0, 0, meanMape, meanRmse,
                new ArrayList<>(Collections.nCopies(periods, mean)),
                new ArrayList<>(Collections.nCopies(periods, mean))
        );

        // AR(1)
        ForecastResult ar1Result = ar1.forecast(history, periods);

        return (ar1Result.getRmse() <= meanResult.getRmse()) ? ar1Result : meanResult;
    }
}
