package com.exampleepam.restaurant.forecast;

import com.exampleepam.restaurant.service.forecast.ArimaModel;
import com.exampleepam.restaurant.service.forecast.ForecastEvaluator;
import com.exampleepam.restaurant.service.forecast.ForecastResult;
import com.exampleepam.restaurant.service.forecast.HoltWintersModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Forecast models")
class ForecastModelTest {

    private static final int FORECAST_HORIZON_1 = 1;
    private static final int FORECAST_HORIZON_3 = 3;
    private static final int FORECAST_HORIZON_4 = 4;

    private static final int SEASON_PERIOD_2 = 2;
    private static final int SEASON_PERIOD_4 = 4;
    private static final int SEASON_PERIOD_12 = 12;

    private static final double EXACT_TOLERANCE = 1e-9;
    private static final double SEASONAL_TOLERANCE = 2.0;

    // ---- helpers ----
    private static List<Integer> history(int... values) {
        return java.util.Arrays.stream(values).boxed().toList();
    }


    @Nested
    @DisplayName("Holt-Winters (triple exponential smoothing)")
    class HoltWinters {

        @Test
        @DisplayName("returns empty forecast when history is empty")
        void returns_emptyForecast_whenHistoryIsEmpty() {
            HoltWintersModel model = new HoltWintersModel(SEASON_PERIOD_12);

            ForecastResult r = model.forecast(List.of(), FORECAST_HORIZON_3);

            assertNotNull(r.getForecasts());
            assertTrue(r.getForecasts().isEmpty(), "Expected empty forecast list");
        }

        @Test
        @DisplayName("repeats last observation when history is shorter than two full seasons")
        void repeats_lastObservation_whenHistoryTooShortForSeasonality() {
            HoltWintersModel model = new HoltWintersModel(SEASON_PERIOD_12);
            List<Integer> shortHistory = history(3, 4, 5, 6); // << 2 * season period

            ForecastResult r = model.forecast(shortHistory, FORECAST_HORIZON_3);

            assertEquals(FORECAST_HORIZON_3, r.getForecasts().size());
            double last = shortHistory.get(shortHistory.size() - 1);
            r.getForecasts().forEach(v -> assertEquals(last, v, EXACT_TOLERANCE));
        }

        @Test
        @DisplayName("tracks a repeating seasonal pattern (period = 2)")
        void tracks_repeatingSeasonalPattern() {
            HoltWintersModel model = new HoltWintersModel(SEASON_PERIOD_2);
            List<Integer> repeating = history(10, 20, 10, 20, 10, 20, 10, 20);

            ForecastResult r = model.forecast(repeating, FORECAST_HORIZON_4);

            assertEquals(FORECAST_HORIZON_4, r.getForecasts().size());
            assertEquals(10.0, r.getForecasts().get(0), SEASONAL_TOLERANCE);
            assertEquals(20.0, r.getForecasts().get(1), SEASONAL_TOLERANCE);
            assertEquals(10.0, r.getForecasts().get(2), SEASONAL_TOLERANCE);
            assertEquals(20.0, r.getForecasts().get(3), SEASONAL_TOLERANCE);

            assertConfidenceIntervalsBracketForecast(r);
        }

        @Test
        @DisplayName("carries trend + seasonality forward (period = 4)")
        void carries_trendAndSeasonality_forward() {
            HoltWintersModel model = new HoltWintersModel(SEASON_PERIOD_4);
            List<Integer> history = history(
                    100, 120, 110, 130,
                    110, 130, 120, 140,
                    120, 140, 130, 150
            );

            ForecastResult r = model.forecast(history, FORECAST_HORIZON_4);

            assertEquals(FORECAST_HORIZON_4, r.getForecasts().size());
            assertTrue(r.getForecasts().get(0) < r.getForecasts().get(1), "Expected rising within-season trend");
            assertTrue(r.getForecasts().get(2) < r.getForecasts().get(3), "Expected rising within-season trend");
            assertTrue(r.getForecasts().get(3) >= 150.0, "Expected continuation at/above last peak");

            assertConfidenceIntervalsBracketForecast(r);
        }

        @Test
        @DisplayName("never returns negative forecasts or confidence bounds")
        void never_returns_negativeValues() {
            HoltWintersModel model = new HoltWintersModel(SEASON_PERIOD_2);
            List<Integer> history = history(10, 0, 8, 0, 6, 0, 4, 0);

            ForecastResult r = model.forecast(history, FORECAST_HORIZON_4);

            assertEquals(FORECAST_HORIZON_4, r.getForecasts().size());
            r.getForecasts().forEach(v -> assertTrue(v >= 0.0, "Forecast must be non-negative"));
            r.getLower().forEach(v -> assertTrue(v >= 0.0, "Lower CI must be non-negative"));
            r.getUpper().forEach(v -> assertTrue(v >= 0.0, "Upper CI must be non-negative"));

            assertConfidenceIntervalsBracketForecast(r);
        }
    }

    @Nested
    @DisplayName("ARIMA (AR(1) with OLS)")
    class Arima {

        @Test
        @DisplayName("returns zeros for empty history (size = requested horizon)")
        void returns_zeros_whenHistoryIsEmpty() {
            ArimaModel model = new ArimaModel();

            ForecastResult r = model.forecast(List.of(), FORECAST_HORIZON_3);

            assertEquals(FORECAST_HORIZON_3, r.getForecasts().size());
            r.getForecasts().forEach(v -> assertEquals(0.0, v, EXACT_TOLERANCE));
            assertConfidenceIntervalsBracketForecast(r);
        }

        @Test
        @DisplayName("repeats single observation for all forecast steps")
        void repeats_singleObservation() {
            ArimaModel model = new ArimaModel();
            int observedValue = 7;

            ForecastResult r = model.forecast(history(observedValue), FORECAST_HORIZON_3);

            assertEquals(FORECAST_HORIZON_3, r.getForecasts().size());
            r.getForecasts().forEach(v -> assertEquals(observedValue, v, EXACT_TOLERANCE));
            assertConfidenceIntervalsBracketForecast(r);
        }

        @Test
        @DisplayName("keeps flat series flat")
        void keeps_flatSeries_flat() {
            ArimaModel model = new ArimaModel();

            ForecastResult r = model.forecast(history(5, 5, 5, 5), FORECAST_HORIZON_1);

            assertEquals(FORECAST_HORIZON_1, r.getForecasts().size());
            assertEquals(5.0, r.getForecasts().get(0), EXACT_TOLERANCE);
            assertConfidenceIntervalsBracketForecast(r);
        }

        @Test
        @DisplayName("matches a perfect AR(1) process: y(t) = 0.5 * y(t-1)")
        void matches_perfect_ar1_process() {
            ArimaModel model = new ArimaModel();

            // Perfect AR(1): 16 -> 8 -> 4 -> 2 -> 1
            List<Integer> ar1 = history(16, 8, 4, 2, 1);

            ForecastResult r = model.forecast(ar1, FORECAST_HORIZON_3);

            assertEquals(FORECAST_HORIZON_3, r.getForecasts().size());
            assertEquals(0.5, r.getForecasts().get(0), EXACT_TOLERANCE);
            assertEquals(0.25, r.getForecasts().get(1), EXACT_TOLERANCE);
            assertEquals(0.125, r.getForecasts().get(2), EXACT_TOLERANCE);

            assertConfidenceIntervalsBracketForecast(r);
        }
    }

    @Test
    @DisplayName("cross-validation returns NaN metrics for sparse / mostly-zero history")
    void crossValidation_returnsNaN_forSparseHistory() {
        List<Integer> sparse = history(0, 5, 0);
        HoltWintersModel model = new HoltWintersModel(SEASON_PERIOD_12);

        ForecastEvaluator.Metrics m = ForecastEvaluator.crossValidate(sparse, model, FORECAST_HORIZON_3);

        assertTrue(Double.isNaN(m.mape()), "MAPE should be NaN for sparse history");
        assertTrue(Double.isNaN(m.rmse()), "RMSE should be NaN for sparse history");
    }

    private static void assertSizesMatch(String label, List<?> a, List<?> b) {
        assertEquals(a.size(), b.size(), label + " sizes must match");
    }

    private static void assertConfidenceIntervalsBracketForecast(ForecastResult r) {
        assertNotNull(r.getForecasts(), "Forecasts must not be null");
        assertNotNull(r.getLower(), "Lower CI must not be null");
        assertNotNull(r.getUpper(), "Upper CI must not be null");

        assertSizesMatch("forecast vs lower", r.getForecasts(), r.getLower());
        assertSizesMatch("forecast vs upper", r.getForecasts(), r.getUpper());

        for (int i = 0; i < r.getForecasts().size(); i++) {
            double f = r.getForecasts().get(i);
            double lo = r.getLower().get(i);
            double hi = r.getUpper().get(i);
            assertTrue(lo <= f, "Lower CI must be <= forecast at index " + i);
            assertTrue(f <= hi, "Forecast must be <= upper CI at index " + i);
        }
    }
}
