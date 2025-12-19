package com.exampleepam.restaurant.forecast;

import com.exampleepam.restaurant.service.forecast.ArimaModel;
import com.exampleepam.restaurant.service.forecast.ForecastResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArimaModelTest {

    private static final int ONE_STEP = 1;
    private static final int THREE_STEPS = 3;
    private static final double TREND_THRESHOLD = 4.5;
    private static final int FLAT_VALUE = 5;
    private static final double SINGLE_OBSERVATION = 7.0;
    private static final List<Integer> TREND_HISTORY = List.of(1, 2, 3, 4, 5);
    private static final List<Integer> FLAT_HISTORY = List.of(FLAT_VALUE, FLAT_VALUE, FLAT_VALUE, FLAT_VALUE);

    @Test
    void arimaProjectsTrend() {
        ArimaModel model = new ArimaModel();
        ForecastResult r = model.forecast(TREND_HISTORY, ONE_STEP);
        assertTrue(r.getForecasts().get(0) > TREND_THRESHOLD);
    }

    @Test
    void arimaHandlesFlatSeries() {
        ArimaModel model = new ArimaModel();
        ForecastResult r = model.forecast(FLAT_HISTORY, ONE_STEP);
        assertEquals(FLAT_VALUE, Math.round(r.getForecasts().get(0)));
    }

    @Test
    void arimaRepeatsSingleObservation() {
        ArimaModel model = new ArimaModel();
        ForecastResult r = model.forecast(List.of((int) SINGLE_OBSERVATION), THREE_STEPS);
        assertEquals(THREE_STEPS, r.getForecasts().size());
        for (double v : r.getForecasts()) {
            assertEquals(SINGLE_OBSERVATION, v);
        }
    }
}
