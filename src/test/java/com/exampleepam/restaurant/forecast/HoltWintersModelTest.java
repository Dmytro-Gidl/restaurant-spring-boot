package com.exampleepam.restaurant.forecast;

import com.exampleepam.restaurant.service.forecast.ForecastEvaluator;
import com.exampleepam.restaurant.service.forecast.ForecastResult;
import com.exampleepam.restaurant.service.forecast.HoltWintersModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HoltWintersModelTest {

    private static final int DEFAULT_PERIOD = 12;
    private static final int SEASONAL_PERIOD = 2;
    private static final int TREND_PERIOD = 4;
    private static final int FORECAST_HORIZON = 4;
    private static final double SEASONAL_TOLERANCE = 2.0;
    private static final int SPARSE_FOLDS = 3;
    private static final int ZERO_VALUE = 0;
    private static final List<Integer> SEASONAL_HISTORY = List.of(10, 20, 10, 20, 10, 20, 10, 20);
    private static final List<Integer> TREND_HISTORY = List.of(
            100, 120, 110, 130,
            110, 130, 120, 140,
            120, 140, 130, 150);
    private static final List<Integer> NEGATIVE_HISTORY = List.of(10, 0, 8, 0, 6, 0, 4, 0);
    private static final double TREND_FLOOR = 150.0;

    @Test
    void holtHandlesEmptyHistory() {
        HoltWintersModel model = new HoltWintersModel(DEFAULT_PERIOD);
        ForecastResult r = model.forecast(List.of(), FORECAST_HORIZON);
        assertEquals(0, r.getForecasts().size());
    }

    @Test
    void crossValidateReturnsNaNForSparseHistory() {
        ForecastEvaluator.Metrics m = ForecastEvaluator.crossValidate(
                List.of(ZERO_VALUE, 5, ZERO_VALUE), new HoltWintersModel(DEFAULT_PERIOD), SPARSE_FOLDS);
        assertTrue(Double.isNaN(m.mape()));
        assertTrue(Double.isNaN(m.rmse()));
    }

    @Test
    void holtWintersRepeatsSeasonalPattern() {
        HoltWintersModel model = new HoltWintersModel(SEASONAL_PERIOD);
        ForecastResult result = model.forecast(SEASONAL_HISTORY, FORECAST_HORIZON);

        assertEquals(FORECAST_HORIZON, result.getForecasts().size());
        assertEquals(10.0, result.getForecasts().get(0), SEASONAL_TOLERANCE);
        assertEquals(20.0, result.getForecasts().get(1), SEASONAL_TOLERANCE);
        assertEquals(10.0, result.getForecasts().get(2), SEASONAL_TOLERANCE);
        assertEquals(20.0, result.getForecasts().get(3), SEASONAL_TOLERANCE);
    }

    @Test
    void holtWintersCarriesTrendAndSeasonality() {
        HoltWintersModel model = new HoltWintersModel(TREND_PERIOD);
        ForecastResult result = model.forecast(TREND_HISTORY, FORECAST_HORIZON);

        assertEquals(FORECAST_HORIZON, result.getForecasts().size());
        assertTrue(result.getForecasts().get(0) < result.getForecasts().get(1));
        assertTrue(result.getForecasts().get(2) < result.getForecasts().get(3));
        assertTrue(result.getForecasts().get(3) >= TREND_FLOOR);
    }

    @Test
    void holtWintersClampsNegativeForecasts() {
        HoltWintersModel model = new HoltWintersModel(SEASONAL_PERIOD);
        ForecastResult result = model.forecast(NEGATIVE_HISTORY, FORECAST_HORIZON);

        assertEquals(FORECAST_HORIZON, result.getForecasts().size());
        for (double forecast : result.getForecasts()) {
            assertTrue(forecast >= 0);
        }
    }
}
