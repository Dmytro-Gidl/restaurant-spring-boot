package com.exampleepam.restaurant.forecast;

import com.exampleepam.restaurant.service.forecast.HoltWintersModel;
import com.exampleepam.restaurant.service.forecast.ArimaModel;
import com.exampleepam.restaurant.service.forecast.ForecastResult;
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
}
