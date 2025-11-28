package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.service.forecast.ForecastModel;
import com.exampleepam.restaurant.service.forecast.ForecastResult;
import com.exampleepam.restaurant.service.forecast.ForecastEvaluator;
import com.exampleepam.restaurant.service.forecast.HistoryCollector;
import com.exampleepam.restaurant.service.forecast.ScaleData;
import com.exampleepam.restaurant.service.forecast.MonthlyResult;
import com.exampleepam.restaurant.service.forecast.MonthlyForecaster;
import com.exampleepam.restaurant.service.forecast.DailyForecaster;
import com.exampleepam.restaurant.service.forecast.HourlyForecaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that produces demand forecasts for dishes. Forecasting uses Holt's
 * linear trend (double exponential smoothing) with smoothing parameters chosen
 * via grid search to minimise one-step-ahead error, allowing recent orders to
 * shape both the expected level and the trajectory of future demand. The
 * resulting predictions are rounded to integers since fractional portions of
 * dishes cannot be prepared.
 */
@Service
public class DishForecastService {

    private static final Logger log = LoggerFactory.getLogger(DishForecastService.class);

    private final DishRepository dishRepository;
    private final HistoryCollector historyCollector;
    private final MonthlyForecaster monthlyForecaster;
    private final DailyForecaster dailyForecaster;
    private final HourlyForecaster hourlyForecaster;
    private final Map<String, ForecastModel> models;
    private final Map<String, Map<Long, ForecastResult>> latestResults = new HashMap<>();
    private final Map<String, Map<Long, List<Integer>>> latestHistory = new HashMap<>();
    private final Map<String, ForecastEvaluator.Metrics> modelMetrics = new HashMap<>();
    private final Map<String, Map<Long, Boolean>> singlePointFlags = new HashMap<>();
    private final Map<String, Map<Long, Boolean>> noDataFlags = new HashMap<>();

    @Autowired
    public DishForecastService(DishRepository dishRepository,
                               HistoryCollector historyCollector,
                               MonthlyForecaster monthlyForecaster,
                               DailyForecaster dailyForecaster,
                               HourlyForecaster hourlyForecaster,
                               List<ForecastModel> models) {
        this.dishRepository = dishRepository;
        this.historyCollector = historyCollector;
        this.monthlyForecaster = monthlyForecaster;
        this.dailyForecaster = dailyForecaster;
        this.hourlyForecaster = hourlyForecaster;
        this.models = models.stream().collect(Collectors.toMap(ForecastModel::getName, m -> m));
    }

    /**
     * Builds forecast data for dishes across hourly, daily and monthly ranges.
     *
     * @param historyDays number of previous days to analyse for hourly trends
     * @param filter      optional dish name filter
     * @param type        optional dish category filter
     * @return page of forecast DTOs, possibly empty
     */
    @Transactional
    public Page<DishForecastDto> getDishForecasts(int historyDays, String filter, Category type,
                                                  String modelName, Pageable pageable) {
        return getDishForecasts(historyDays, filter, type, modelName, pageable, false);
    }

    @Transactional
    public Page<DishForecastDto> getDishForecasts(int historyDays, String filter, Category type,
                                                  String modelName, Pageable pageable, boolean persist) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusYears(2).atStartOfDay();

        // 1. Load order history and aggregate to hourly/daily/monthly totals.
        HistoryCollector.History history = historyCollector.collect(start);
        // evaluate models once per request using global monthly totals
        List<Integer> globalMonths = history.globalMonthlyTotals();
        for (var e : models.entrySet()) {
            ForecastEvaluator.Metrics m = ForecastEvaluator.crossValidate(globalMonths, e.getValue(), 3);
            modelMetrics.put(e.getKey(), m);
            log.info("Model {} CV MAPE={} RMSE={}", e.getKey(), m.mape(), m.rmse());
        }

        // 2. Fetch dishes subject to optional filters.
        Pageable sortedPageable = (pageable == null || pageable.isUnpaged())
                ? Pageable.unpaged()
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
        Page<Dish> dishes = loadDishes(filter, type, sortedPageable);

        // 3. Build forecast DTOs for each dish.
        List<DishForecastDto> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Dish dish : dishes.getContent()) {
            result.add(buildForecastForDish(dish, history, today, now, historyDays, modelName, persist));
        }

        return new PageImpl<>(result, pageable, dishes.getTotalElements());
    }

    /** Fetches dishes applying optional name and category filters. */
    private Page<Dish> loadDishes(String filter, Category type, Pageable pageable) {
        if ((filter == null || filter.isBlank()) && type == null) {
            return dishRepository.findAllByArchivedFalse(pageable);
        } else if (filter == null || filter.isBlank()) {
            return dishRepository.findByCategoryAndArchivedFalse(type, pageable);
        } else if (type == null) {
            return dishRepository.findByNameContainingIgnoreCaseAndArchivedFalse(filter, pageable);
        } else {
            return dishRepository.findByNameContainingIgnoreCaseAndCategoryAndArchivedFalse(filter, type, pageable);
        }
    }

    /**
     * Builds forecasts for a single dish. The method generates monthly
     * predictions using Holt's linear trend and then distributes the monthly
     * values down to days and hours.
     */
    private DishForecastDto buildForecastForDish(Dish dish, HistoryCollector.History history, LocalDate today,
                                                LocalDateTime now, int historyDays, String modelName, boolean persist) {
        long id = dish.getId();

        Map<String, List<String>> labelsMap = new HashMap<>();
        Map<String, List<Integer>> actualMap = new HashMap<>();
        Map<String, List<Integer>> forecastMap = new HashMap<>();

        MonthlyResult monthResult = monthlyForecaster.forecast(dish, history, models.get(modelName), persist);
        if (monthResult.noData()) {
            log.warn("Dish {} has no completed order history", id);
        }
        latestResults.computeIfAbsent(modelName, k -> new HashMap<>()).put(id, monthResult.result());
        latestHistory.computeIfAbsent(modelName, k -> new HashMap<>()).put(id, monthResult.modelHistory());
        singlePointFlags.computeIfAbsent(modelName, k -> new HashMap<>()).put(id, monthResult.singlePoint());
        noDataFlags.computeIfAbsent(modelName, k -> new HashMap<>()).put(id, monthResult.noData());
        labelsMap.put("monthly", monthResult.scale().labels());
        actualMap.put("monthly", monthResult.scale().actual());
        forecastMap.put("monthly", monthResult.scale().forecast());

        ScaleData daily = dailyForecaster.forecast(id, history, today, monthResult.monthForecasts());
        labelsMap.put("daily", daily.labels());
        actualMap.put("daily", daily.actual());
        forecastMap.put("daily", daily.forecast());

        ScaleData hourly = hourlyForecaster.forecast(id, history, today, now, daily, historyDays);
        labelsMap.put("hourly", hourly.labels());
        actualMap.put("hourly", hourly.actual());
        forecastMap.put("hourly", hourly.forecast());

        return new DishForecastDto(id, dish.getName(), dish.getimagePath(), labelsMap, actualMap, forecastMap,
                monthResult.singlePoint(), monthResult.noData());
    }

    public ForecastDetails getDetails(String modelName, long dishId) {
        Map<Long, List<Integer>> h = latestHistory.getOrDefault(modelName, Map.of());
        Map<Long, ForecastResult> r = latestResults.getOrDefault(modelName, Map.of());
        Map<Long, Boolean> sp = singlePointFlags.getOrDefault(modelName, Map.of());
        Map<Long, Boolean> nd = noDataFlags.getOrDefault(modelName, Map.of());
        return new ForecastDetails(h.getOrDefault(dishId, List.of()),
                r.get(dishId), sp.getOrDefault(dishId, false), nd.getOrDefault(dishId, false));
    }

    public record ForecastDetails(List<Integer> history, ForecastResult result, boolean singlePoint, boolean noData) {}

    public Map<String, ForecastEvaluator.Metrics> getModelMetrics() {
        return modelMetrics;
    }

    // Legacy Holt linear helpers removed in favour of ForecastModel implementation.
}
