package com.exampleepam.restaurant.service.forecast;

import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.DishForecast;
import com.exampleepam.restaurant.repository.DishForecastRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MonthlyForecaster {

    private final DishForecastRepository forecastRepository;

    @Autowired
    public MonthlyForecaster(DishForecastRepository forecastRepository) {
        this.forecastRepository = forecastRepository;
    }

    public MonthlyResult forecast(Dish dish,
                                  HistoryCollector.History history,
                                  ForecastModel model) {
        long id = dish.getId();
        Map<YearMonth, Integer> dishMonthly = history.monthlyTotals.getOrDefault(id, Map.of());
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(24);

        List<String> labels = new ArrayList<>();
        List<Integer> actual = new ArrayList<>();
        List<Integer> forecast = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            YearMonth ym = startMonth.plusMonths(i);
            int val = dishMonthly.getOrDefault(ym, 0);
            labels.add(ym.toString());
            actual.add(val);
            forecast.add(null);
        }
        int currentVal = dishMonthly.getOrDefault(currentMonth, 0);
        labels.add(currentMonth.toString());
        actual.add(currentVal);
        forecast.add(null);

        List<Integer> modelHistory = new ArrayList<>(actual);
        int trimmed = 0;
        while (!modelHistory.isEmpty() && modelHistory.get(0) == 0) {
            modelHistory.remove(0);
            trimmed++;
        }
        if (modelHistory.isEmpty()) {
            modelHistory.add(currentVal);
        }
        boolean singlePoint = modelHistory.size() == 1;
        ForecastResult result = model.forecast(modelHistory, 12);
        Map<YearMonth, Integer> monthForecastMap = new HashMap<>();
        for (int i = 0; i < result.getForecasts().size(); i++) {
            YearMonth ym = currentMonth.plusMonths(i + 1);
            int pred = (int) Math.round(result.getForecasts().get(i));
            monthForecastMap.put(ym, pred);
            DishForecast df = new DishForecast();
            df.setDish(dish);
            df.setDate(ym.atDay(1));
            df.setQuantity(pred);
            df.setGeneratedAt(java.time.LocalDate.now());
            forecastRepository.save(df);
            labels.add(ym.toString());
            actual.add(null);
            forecast.add(pred);
        }
        return new MonthlyResult(new ScaleData(labels, actual, forecast), monthForecastMap, modelHistory, result, singlePoint);
    }
}
