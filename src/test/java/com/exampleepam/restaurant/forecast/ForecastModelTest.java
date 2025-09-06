package com.exampleepam.restaurant.forecast;

import com.exampleepam.restaurant.service.forecast.HoltWintersModel;
import com.exampleepam.restaurant.service.forecast.ArimaModel;
import com.exampleepam.restaurant.service.forecast.ForecastResult;
import com.exampleepam.restaurant.service.forecast.MonthlyForecaster;
import com.exampleepam.restaurant.service.forecast.HistoryCollector;
import com.exampleepam.restaurant.service.forecast.ForecastEvaluator;
import com.exampleepam.restaurant.service.forecast.ForecastModel;
import com.exampleepam.restaurant.service.forecast.MonthlyResult;
import com.exampleepam.restaurant.service.forecast.ScaleData;
import com.exampleepam.restaurant.repository.DishForecastRepository;
import com.exampleepam.restaurant.entity.Dish;

import org.mockito.Mockito;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ForecastModelTest {

    @Test
    void holtHandlesEmptyHistory() {
        HoltWintersModel model = new HoltWintersModel(12);
        ForecastResult r = model.forecast(List.of(), 3);
        assertEquals(0, r.getForecasts().size());
    }

    @Test
    void arimaProjectsTrend() {
        ArimaModel model = new ArimaModel();
        ForecastResult r = model.forecast(List.of(1,2,3,4,5), 1);
        assertTrue(r.getForecasts().get(0) > 4.5);
    }

    @Test
    void arimaHandlesFlatSeries() {
        ArimaModel model = new ArimaModel();
        ForecastResult r = model.forecast(List.of(5,5,5,5), 1);
        assertEquals(5, Math.round(r.getForecasts().get(0)));
    }

    @Test
    void arimaRepeatsSingleObservation() {
        ArimaModel model = new ArimaModel();
        ForecastResult r = model.forecast(List.of(7), 3);
        assertEquals(3, r.getForecasts().size());
        for (double v : r.getForecasts()) {
            assertEquals(7.0, v);
        }
    }

    @Test
    void monthlyForecasterTrimsZerosAndFlagsSinglePoint() {
        DishForecastRepository repo = Mockito.mock(DishForecastRepository.class);
        MonthlyForecaster forecaster = new MonthlyForecaster(repo);
        HistoryCollector.History history = new HistoryCollector.History();
        long dishId = 1L;
        java.time.YearMonth ym = java.time.YearMonth.now();
        history.monthlyTotals.put(dishId, java.util.Map.of(ym, 5));
        Dish dish = new Dish();
        dish.setId(dishId);
        ForecastModel stubModel = new ForecastModel() {
            @Override public String getName() { return "stub"; }
            @Override public ForecastResult forecast(List<Integer> h, int p) { return new ForecastResult(java.util.Collections.nCopies(p, 1.0), List.of(), List.of(),0,0,0); }
        };
        MonthlyResult result = forecaster.forecast(dish, history, stubModel);
        ScaleData scale = result.scale();
        assertEquals(0, scale.actual().get(0));
        assertEquals(List.of(5), result.modelHistory());
        assertTrue(result.singlePoint());
    }

    @Test
    void crossValidateReturnsNaNForSparseHistory() {
        ForecastEvaluator.Metrics m = ForecastEvaluator.crossValidate(List.of(0, 5, 0), new HoltWintersModel(12), 3);
        assertTrue(Double.isNaN(m.mape()));
        assertTrue(Double.isNaN(m.rmse()));
    }

    @Test
    void monthlyForecasterKeepsActualsAndAppendsForecasts() {
        DishForecastRepository repo = Mockito.mock(DishForecastRepository.class);
        MonthlyForecaster forecaster = new MonthlyForecaster(repo);
        HistoryCollector.History history = new HistoryCollector.History();
        long dishId = 2L;
        java.time.YearMonth ym = java.time.YearMonth.now();
        history.monthlyTotals.put(dishId, java.util.Map.of(ym, 4));
        Dish dish = new Dish();
        dish.setId(dishId);
        ForecastModel stubModel = new ForecastModel() {
            @Override public String getName() { return "stub"; }
            @Override public ForecastResult forecast(List<Integer> h, int p) { return new ForecastResult(java.util.Collections.nCopies(p, 2.0), List.of(), List.of(),0,0,0); }
        };
        MonthlyResult result = forecaster.forecast(dish, history, stubModel);
        ScaleData scale = result.scale();
        int historyIndex = scale.actual().indexOf(4);
        assertEquals(4, scale.actual().get(historyIndex));
        assertNull(scale.forecast().get(historyIndex));
        assertEquals(2, scale.forecast().get(historyIndex + 1));
    }
}
