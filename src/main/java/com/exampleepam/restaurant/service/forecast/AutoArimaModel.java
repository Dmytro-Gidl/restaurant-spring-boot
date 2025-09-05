package com.exampleepam.restaurant.service.forecast;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Very small automatic ARIMA selector that compares ARIMA(0,0,0)
 * (equivalent to a naive mean model) against the ARIMA(1,0,0)
 * implementation and returns the one with lower RMSE on the
 * training data.
 */
@Component
public class AutoArimaModel implements ForecastModel {

    private final ArimaModel ar1 = new ArimaModel();

    @Override
    public String getName() {
        return "auto-arima";
    }

    @Override
    public ForecastResult forecast(List<Integer> history, int periods) {
        // Candidate 1: mean model
        double mean = history.stream().mapToDouble(Integer::doubleValue).average().orElse(0);
        List<Double> meanForecasts = new ArrayList<>(Collections.nCopies(periods, mean));
        List<Double> meanActual = new ArrayList<>();
        List<Double> meanFitted = new ArrayList<>();
        for (double v : history) {
            meanActual.add(v);
            meanFitted.add(mean);
        }
        double meanMape = ForecastEvaluator.mape(meanActual, meanFitted);
        double meanRmse = ForecastEvaluator.rmse(meanActual, meanFitted);
        ForecastResult meanResult = new ForecastResult(meanForecasts, 0,0,0, meanMape, meanRmse,
                Collections.nCopies(periods, mean), Collections.nCopies(periods, mean));

        // Candidate 2: ARIMA(1,0,0)
        ForecastResult ar1Result = ar1.forecast(history, periods);

        // Choose the model with smaller RMSE
        if (ar1Result.getRmse() <= meanResult.getRmse()) {
            return ar1Result;
        } else {
            return meanResult;
        }
    }
}
