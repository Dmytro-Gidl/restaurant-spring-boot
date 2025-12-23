package com.exampleepam.restaurant.service.forecast;

import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.DishForecast;
import com.exampleepam.restaurant.repository.DishForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyForecaster {

    private final DishForecastRepository forecastRepository;

    private static final int MONTH_WINDOW = 36;
    private static final int FORECAST_HORIZON = 12;

    public MonthlyResult forecast(Dish dish,
                                  HistoryCollector.History history,
                                  ForecastModel model,
                                  boolean persist) {

        long dishId = dish.getId();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(MONTH_WINDOW);

        Map<YearMonth, Integer> dishMonthly = history.monthlyTotals.getOrDefault(dishId, Map.of());
        boolean noData = dishMonthly.isEmpty();
        if (noData) log.warn("Dish {} has no completed orders in history", dishId);

        // Build base window (36 months) + current month
        List<String> baseLabels = new ArrayList<>(MONTH_WINDOW + 1);
        List<Integer> baseActual = new ArrayList<>(MONTH_WINDOW + 1);
        List<Integer> baseForecast = new ArrayList<>(MONTH_WINDOW + 1);

        for (int i = 0; i < MONTH_WINDOW; i++) {
            YearMonth ym = startMonth.plusMonths(i);
            baseLabels.add(ym.toString());
            baseActual.add(dishMonthly.getOrDefault(ym, 0));
            baseForecast.add(null);
        }

        int currentVal = dishMonthly.getOrDefault(currentMonth, 0);
        baseLabels.add(currentMonth.toString());
        baseActual.add(currentVal);
        baseForecast.add(null);

        // Trim leading/trailing zeros WITHOUT O(n^2) removes
        Trim trim = trimZeros(baseActual);
        List<Integer> modelHistory = new ArrayList<>(baseActual.subList(trim.fromInclusive, trim.toExclusive));

        if (trim.leading > 0) log.debug("Dish {} trimmed {} leading zero months", dishId, trim.leading);
        if (trim.trailing > 0) log.debug("Dish {} trimmed {} trailing zero months", dishId, trim.trailing);

        if (modelHistory.isEmpty()) {
            log.warn("Dish {} model history empty after trimming; using current month value", dishId);
            modelHistory.add(currentVal);
        }

        boolean singlePoint = modelHistory.size() == 1;
        if (singlePoint && !noData) {
            log.warn("Dish {} has a single data point; forecasts will repeat this value", dishId);
        }

        ForecastResult result = model.forecast(modelHistory, FORECAST_HORIZON);
        boolean emptyForecast = result.getForecasts() == null || result.getForecasts().isEmpty();
        if (emptyForecast) {
            log.warn("Dish {} model {} returned no forecasts; leaving projection empty", dishId, model.getName());
        }

        Map<YearMonth, Integer> monthForecastMap = new HashMap<>();
        List<String> displayLabels = new ArrayList<>(baseLabels);
        List<Integer> displayActual = new ArrayList<>(baseActual);
        List<Integer> displayForecast = new ArrayList<>(baseForecast);

        if (persist) {
            forecastRepository.deleteByDishAndGeneratedAt(dish, today);
        }

        if (!emptyForecast) {
            List<DishForecast> toSave = persist ? new ArrayList<>(result.getForecasts().size()) : null;

            for (int i = 0; i < result.getForecasts().size(); i++) {
                YearMonth ym = currentMonth.plusMonths(i + 1);

                int pred = safeNonNegativeInt(result.getForecasts().get(i)); // clamp negatives
                monthForecastMap.put(ym, pred);

                displayLabels.add(ym.toString());
                displayActual.add(null);
                displayForecast.add(pred);

                if (persist) {
                    DishForecast df = new DishForecast();
                    df.setDish(dish);
                    df.setDate(ym.atDay(1));
                    df.setQuantity(pred);
                    df.setGeneratedAt(today);
                    toSave.add(df);
                }
            }

            if (persist && !toSave.isEmpty()) {
                forecastRepository.saveAll(toSave);
            }
        }

        return new MonthlyResult(
                new ScaleData(displayLabels, displayActual, displayForecast),
                monthForecastMap,
                modelHistory,
                result,
                singlePoint,
                noData,
                emptyForecast
        );
    }

    private static int safeNonNegativeInt(Double v) {
        if (v == null) return 0;
        long rounded = Math.round(v);
        if (rounded < 0) return 0;
        if (rounded > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) rounded;
    }

    private static Trim trimZeros(List<Integer> series) {
        int n = series.size();
        int from = 0;
        while (from < n && safeVal(series.get(from)) == 0) from++;

        int to = n;
        while (to > Math.max(from + 1, 1) && safeVal(series.get(to - 1)) == 0) to--;

        int leading = from;
        int trailing = n - to;
        return new Trim(from, to, leading, trailing);
    }

    private static int safeVal(Integer v) {
        return v == null ? 0 : v;
    }

    private record Trim(int fromInclusive, int toExclusive, int leading, int trailing) {}
}
