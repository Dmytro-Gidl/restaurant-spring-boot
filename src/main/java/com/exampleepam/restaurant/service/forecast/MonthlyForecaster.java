package com.exampleepam.restaurant.service.forecast;

import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.DishForecast;
import com.exampleepam.restaurant.repository.DishForecastRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MonthlyForecaster {

    private final DishForecastRepository forecastRepository;
    private static final Logger log = LoggerFactory.getLogger(MonthlyForecaster.class);

    @Autowired
    public MonthlyForecaster(DishForecastRepository forecastRepository) {
        this.forecastRepository = forecastRepository;
    }

    public MonthlyResult forecast(Dish dish,
                                  HistoryCollector.History history,
                                  ForecastModel model,
                                  boolean persist) {
        long id = dish.getId();
        Map<YearMonth, Integer> dishMonthly = history.monthlyTotals.getOrDefault(id, Map.of());
        boolean noData = dishMonthly.isEmpty();
        if (noData) {
            log.warn("Dish {} has no completed orders in history", id);
        }
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(24);

        List<String> baseLabels = new ArrayList<>();
        List<Integer> baseActual = new ArrayList<>();
        List<Integer> forecast = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            YearMonth ym = startMonth.plusMonths(i);
            int val = dishMonthly.getOrDefault(ym, 0);
            baseLabels.add(ym.toString());
            baseActual.add(val);
            forecast.add(null);
        }
        int currentVal = dishMonthly.getOrDefault(currentMonth, 0);
        baseLabels.add(currentMonth.toString());
        baseActual.add(currentVal);
        forecast.add(null);

        // Use trimmed history for modelling but keep the full arrays for display
        List<Integer> modelHistory = new ArrayList<>(baseActual);
        int trimmed = 0;
        while (!modelHistory.isEmpty() && modelHistory.get(0) == 0) {
            modelHistory.remove(0);
            trimmed++;
        }
        int trailing = 0;
        while (modelHistory.size() > 1 && modelHistory.get(modelHistory.size() - 1) == 0) {
            modelHistory.remove(modelHistory.size() - 1);
            trailing++;
        }
        log.debug("Dish {} trimmed {} leading zero months", id, trimmed);
        if (trailing > 0) {
            log.debug("Dish {} trimmed {} trailing zero months", id, trailing);
        }
        if (modelHistory.isEmpty()) {
            log.warn("Dish {} model history empty after trimming; using current month value", id);
            modelHistory.add(currentVal);
        }
        log.debug("Dish {} model history {}", id, modelHistory);
        boolean singlePoint = modelHistory.size() == 1;
        if (singlePoint && !noData) {
            log.warn("Dish {} has a single data point; forecasts will repeat this value", id);
        }
        ForecastResult result = model.forecast(modelHistory, 12);
        boolean emptyForecast = result.getForecasts().isEmpty();
        if (emptyForecast) {
            log.warn("Dish {} model {} returned no forecasts; leaving projection empty", id, model.getName());
        }
        log.debug("Dish {} predictions {}", id, result.getForecasts());
        Map<YearMonth, Integer> monthForecastMap = new HashMap<>();
        List<String> displayLabels = new ArrayList<>(baseLabels);
        List<Integer> displayActual = new ArrayList<>(baseActual);
        if (persist) {
            forecastRepository.deleteByDishAndGeneratedAt(dish, java.time.LocalDate.now());
        }
        for (int i = 0; i < result.getForecasts().size(); i++) {
            YearMonth ym = currentMonth.plusMonths(i + 1);
            int pred = (int) Math.round(result.getForecasts().get(i));
            monthForecastMap.put(ym, pred);
            if (persist) {
                DishForecast df = new DishForecast();
                df.setDish(dish);
                df.setDate(ym.atDay(1));
                df.setQuantity(pred);
                df.setGeneratedAt(java.time.LocalDate.now());
                forecastRepository.save(df);
            }
            displayLabels.add(ym.toString());
            displayActual.add(null);
            forecast.add(pred);
        }
        return new MonthlyResult(new ScaleData(displayLabels, displayActual, forecast), monthForecastMap, modelHistory, result, singlePoint, noData, emptyForecast);
    }
}
