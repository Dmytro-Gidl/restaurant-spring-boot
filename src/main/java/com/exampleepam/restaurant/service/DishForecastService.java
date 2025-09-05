package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.entity.DishForecast;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.OrderRepository;
import com.exampleepam.restaurant.repository.DishForecastRepository;
import com.exampleepam.restaurant.service.forecast.ForecastModel;
import com.exampleepam.restaurant.service.forecast.ForecastResult;
import com.exampleepam.restaurant.service.forecast.ForecastEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final DishForecastRepository forecastRepository;
    private final Map<String, ForecastModel> models;
    private final Map<String, Map<Long, ForecastResult>> latestResults = new HashMap<>();
    private final Map<String, Map<Long, List<Integer>>> latestHistory = new HashMap<>();
    private final Map<String, ForecastEvaluator.Metrics> modelMetrics = new HashMap<>();

    @Autowired
    public DishForecastService(OrderRepository orderRepository,
                               DishRepository dishRepository,
                               DishForecastRepository forecastRepository,
                               List<ForecastModel> models) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
        this.forecastRepository = forecastRepository;
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
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusYears(2).atStartOfDay();

        // 1. Load order history and aggregate to hourly/daily/monthly totals.
        History history = collectHistory(start);
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
            result.add(buildForecastForDish(dish, history, today, now, historyDays, modelName));
        }

        return new PageImpl<>(result, pageable, dishes.getTotalElements());
    }

    /** Aggregates historical orders into hourly, daily and monthly totals. */
    private History collectHistory(LocalDateTime start) {
        List<Order> orders = orderRepository.findByStatusAndCreationDateTimeAfter(Status.COMPLETED, start);
        History history = new History();
        for (Order order : orders) {
            LocalDateTime dateTime = order.getCreationDateTime();
            LocalDate date = dateTime.toLocalDate();
            int hour = dateTime.getHour();
            YearMonth ym = YearMonth.from(dateTime);
            for (OrderItem item : order.getOrderItems()) {
                long dishId = item.getDish().getId();
                int qty = item.getQuantity();
                history.hourlyTotals.computeIfAbsent(dishId, k -> new HashMap<>())
                        .computeIfAbsent(date, d -> new int[24])[hour] += qty;
                history.dailyTotals.computeIfAbsent(dishId, k -> new HashMap<>())
                        .merge(date, qty, Integer::sum);
                history.monthlyTotals.computeIfAbsent(dishId, k -> new HashMap<>())
                        .merge(ym, qty, Integer::sum);
                history.globalMonthly.merge(ym, qty, Integer::sum);
            }
        }
        return history;
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
    private DishForecastDto buildForecastForDish(Dish dish, History history, LocalDate today,
                                                LocalDateTime now, int historyDays, String modelName) {
        long id = dish.getId();

        Map<String, List<String>> labelsMap = new HashMap<>();
        Map<String, List<Integer>> actualMap = new HashMap<>();
        Map<String, List<Integer>> forecastMap = new HashMap<>();

        MonthlyResult monthResult = forecastMonthly(dish, history, modelName);
        labelsMap.put("monthly", monthResult.scale().labels());
        actualMap.put("monthly", monthResult.scale().actual());
        forecastMap.put("monthly", monthResult.scale().forecast());

        ScaleData daily = forecastDaily(id, history, today, monthResult.monthForecasts());
        labelsMap.put("daily", daily.labels());
        actualMap.put("daily", daily.actual());
        forecastMap.put("daily", daily.forecast());

        ScaleData hourly = forecastHourly(id, history, today, now, daily, historyDays);
        labelsMap.put("hourly", hourly.labels());
        actualMap.put("hourly", hourly.actual());
        forecastMap.put("hourly", hourly.forecast());

        return new DishForecastDto(id, dish.getName(), dish.getImagePath(), labelsMap, actualMap, forecastMap);
    }

    private MonthlyResult forecastMonthly(Dish dish, History history, String modelName) {
        long id = dish.getId();
        Map<YearMonth, Integer> dishMonthly = history.monthlyTotals.getOrDefault(id, Map.of());
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(24);

        List<String> labels = new ArrayList<>();
        List<Integer> actual = new ArrayList<>();
        List<Integer> forecast = new ArrayList<>();
        List<Integer> historyMonths = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            YearMonth ym = startMonth.plusMonths(i);
            int val = dishMonthly.getOrDefault(ym, 0);
            labels.add(ym.toString());
            actual.add(val);
            forecast.add(null);
            historyMonths.add(val);
        }
        int currentVal = dishMonthly.getOrDefault(currentMonth, 0);
        labels.add(currentMonth.toString());
        actual.add(currentVal);
        forecast.add(null);
        historyMonths.add(currentVal);

        ForecastModel model = models.getOrDefault(modelName, models.values().iterator().next());
        ForecastResult result = model.forecast(historyMonths, 24);
        latestResults.computeIfAbsent(modelName, k -> new HashMap<>()).put(id, result);
        latestHistory.computeIfAbsent(modelName, k -> new HashMap<>()).put(id, new ArrayList<>(historyMonths));
        log.info("Dish {} forecast alpha={} beta={} gamma={} MAPE={} RMSE={}",
                id, result.getAlpha(), result.getBeta(), result.getGamma(),
                result.getMape(), result.getRmse());
        forecastRepository.deleteByGeneratedAtBefore(LocalDate.now());
        Map<YearMonth, Integer> monthForecastMap = new HashMap<>();
        List<Double> preds = result.getForecasts();
        for (int i = 0; i < preds.size(); i++) {
            YearMonth ym = currentMonth.plusMonths(i + 1);
            int pred = Math.max(0, (int) Math.round(preds.get(i)));
            monthForecastMap.put(ym, pred);
            DishForecast df = new DishForecast();
            df.setDish(dish);
            df.setDate(ym.atDay(1));
            df.setQuantity(pred);
            df.setGeneratedAt(LocalDate.now());
            forecastRepository.save(df);
            labels.add(ym.toString());
            actual.add(null);
            forecast.add(pred);
        }

        return new MonthlyResult(new ScaleData(labels, actual, forecast), monthForecastMap);
    }

    private ScaleData forecastDaily(long id, History history, LocalDate today,
                                   Map<YearMonth, Integer> monthForecastMap) {
        Map<LocalDate, Integer> dishDaily = history.dailyTotals.getOrDefault(id, Map.of());
        YearMonth currentMonth = YearMonth.now();
        List<String> labels = new ArrayList<>();
        List<Integer> actual = new ArrayList<>();
        List<Integer> forecast = new ArrayList<>();

        for (int i = -30; i <= 0; i++) {
            LocalDate day = today.plusDays(i);
            labels.add(day.toString());
            actual.add(dishDaily.getOrDefault(day, 0));
            forecast.add(null);
        }

        Map<YearMonth, Integer> remainingMonthly = new HashMap<>();
        Map<YearMonth, Integer> allocatedDays = new HashMap<>();
        LocalDate futureDay = today.plusDays(1);
        for (int i = 1; i <= 30; i++) {
            YearMonth ym = YearMonth.from(futureDay);
            int monthPred = monthForecastMap.getOrDefault(ym, 0);
            remainingMonthly.computeIfAbsent(ym, m -> {
                int actualSoFar = dishDaily.entrySet().stream()
                        .filter(e -> YearMonth.from(e.getKey()).equals(ym) && !e.getKey().isAfter(today))
                        .mapToInt(Map.Entry::getValue)
                        .sum();
                return Math.max(0, monthPred - actualSoFar);
            });
            allocatedDays.putIfAbsent(ym, ym.equals(currentMonth) ? today.getDayOfMonth() : 0);
            int usedDays = allocatedDays.get(ym);
            int daysInMonth = ym.lengthOfMonth();
            int remainingDays = daysInMonth - usedDays;
            int remainingQty = remainingMonthly.get(ym);
            int base = remainingDays > 0 ? remainingQty / remainingDays : 0;
            int rem = remainingDays > 0 ? remainingQty % remainingDays : 0;
            int dayIndex = usedDays - (ym.equals(currentMonth) ? today.getDayOfMonth() : 0);
            int val = base + (dayIndex < rem ? 1 : 0);
            remainingMonthly.put(ym, remainingQty - val);
            allocatedDays.put(ym, usedDays + 1);
            labels.add(futureDay.toString());
            actual.add(null);
            forecast.add(val);
            futureDay = futureDay.plusDays(1);
        }
        reconcileDaily(monthForecastMap, labels, forecast);
        return new ScaleData(labels, actual, forecast);
    }

    /** Adjust daily predictions so their monthly sums exactly match the monthly forecasts. */
    private void reconcileDaily(Map<YearMonth, Integer> monthForecastMap, List<String> labels, List<Integer> forecast) {
        Map<YearMonth, Integer> sums = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            Integer val = forecast.get(i);
            if (val == null) continue;
            YearMonth ym = YearMonth.parse(labels.get(i).substring(0,7));
            sums.merge(ym, val, Integer::sum);
        }
        for (Map.Entry<YearMonth, Integer> e : monthForecastMap.entrySet()) {
            int diff = e.getValue() - sums.getOrDefault(e.getKey(),0);
            if (diff==0) continue;
            for (int i = labels.size()-1; i>=0; i--) {
                if (forecast.get(i)==null) continue;
                YearMonth ym = YearMonth.parse(labels.get(i).substring(0,7));
                if (ym.equals(e.getKey())) { forecast.set(i, forecast.get(i)+diff); break; }
            }
        }
    }

    private ScaleData forecastHourly(long id, History history, LocalDate today, LocalDateTime now,
                                    ScaleData daily, int historyDays) {
        Map<LocalDate, int[]> dishHours = history.hourlyTotals.getOrDefault(id, Map.of());
        List<int[]> pastArrays = dishHours.entrySet().stream()
                .filter(e -> e.getKey().isBefore(today) && !e.getKey().isBefore(today.minusDays(historyDays)))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        double[] hourWeights = new double[24];
        double total = 0.0;
        for (int[] arr : pastArrays) {
            int dayTotal = Arrays.stream(arr).sum();
            total += dayTotal;
            for (int h = 0; h < 24; h++) {
                hourWeights[h] += arr[h];
            }
        }
        if (total == 0) {
            Arrays.fill(hourWeights, 1.0 / 24.0);
        } else {
            for (int h = 0; h < 24; h++) {
                hourWeights[h] /= total;
            }
        }

        Map<LocalDate, int[]> futureAlloc = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Integer> actual = new ArrayList<>();
        List<Integer> forecast = new ArrayList<>();
        DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("MM-dd HH:00");
        LocalDateTime startHour = today.minusDays(7).atStartOfDay();
        for (int i = 0; i < 24 * 15; i++) {
            LocalDateTime dt = startHour.plusHours(i);
            labels.add(dt.format(hourFmt));
            int hour = dt.getHour();
            LocalDate d = dt.toLocalDate();
            if (dt.isBefore(now)) {
                int[] arr = dishHours.getOrDefault(d, new int[24]);
                actual.add(arr[hour]);
                forecast.add(null);
            } else {
                int idx = daily.labels().indexOf(d.toString());
                int dayPred = (idx >= 0 && idx < daily.forecast().size() && daily.forecast().get(idx) != null)
                        ? daily.forecast().get(idx) : 0;
                int[] alloc = futureAlloc.computeIfAbsent(d, k -> distribute(dayPred, hourWeights));
                actual.add(null);
                forecast.add(alloc[hour]);
            }
        }
        reconcileHourly(daily, labels, forecast);
        return new ScaleData(labels, actual, forecast);
    }

    private void reconcileHourly(ScaleData daily, List<String> labels, List<Integer> forecast) {
        Map<String, Integer> sums = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            Integer val = forecast.get(i);
            if (val == null) continue;
            String day = labels.get(i).substring(0,5); // MM-dd
            sums.merge(day, val, Integer::sum);
        }
        for (int i = 0; i < daily.labels().size(); i++) {
            String day = daily.labels().get(i).substring(5); // yyyy-MM-dd -> MM-dd
            Integer dayPred = daily.forecast().get(i);
            if (dayPred == null) continue;
            int diff = dayPred - sums.getOrDefault(day,0);
            if (diff==0) continue;
            for (int j = labels.size()-1; j>=0; j--) {
                if (forecast.get(j)==null) continue;
                if (labels.get(j).startsWith(day)) { forecast.set(j, forecast.get(j)+diff); break; }
            }
        }
    }

    private record ScaleData(List<String> labels, List<Integer> actual, List<Integer> forecast) {}

    private record MonthlyResult(ScaleData scale, Map<YearMonth, Integer> monthForecasts) {}


    /** Simple container for aggregated history. */
    private static class History {
        final Map<Long, Map<LocalDate, int[]>> hourlyTotals = new HashMap<>();
        final Map<Long, Map<LocalDate, Integer>> dailyTotals = new HashMap<>();
        final Map<Long, Map<YearMonth, Integer>> monthlyTotals = new HashMap<>();
        final Map<YearMonth, Integer> globalMonthly = new HashMap<>();

        List<Integer> globalMonthlyTotals() {
            YearMonth current = YearMonth.now();
            YearMonth start = current.minusMonths(24);
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i <= 24; i++) {
                YearMonth ym = start.plusMonths(i);
                list.add(globalMonthly.getOrDefault(ym, 0));
            }
            return list;
        }
    }

    private int[] distribute(int total, double[] weights) {
        int[] arr = new int[24];
        int remaining = total;
        for (int h = 0; h < 24; h++) {
            int val = (int) Math.floor(total * weights[h]);
            arr[h] = val;
            remaining -= val;
        }
        int[] order = java.util.stream.IntStream.range(0, 24)
                .boxed()
                .sorted((a, b) -> Double.compare(weights[b], weights[a]))
                .mapToInt(Integer::intValue)
                .toArray();
        int idx = 0;
        while (remaining > 0) {
            arr[order[idx % 24]]++;
            remaining--;
            idx++;
        }
        return arr;
    }

    public ForecastDetails getDetails(String modelName, long dishId) {
        Map<Long, List<Integer>> h = latestHistory.getOrDefault(modelName, Map.of());
        Map<Long, ForecastResult> r = latestResults.getOrDefault(modelName, Map.of());
        return new ForecastDetails(h.getOrDefault(dishId, List.of()),
                r.get(dishId));
    }

    public record ForecastDetails(List<Integer> history, ForecastResult result) {}

    public Map<String, ForecastEvaluator.Metrics> getModelMetrics() {
        return modelMetrics;
    }

    // Legacy Holt linear helpers removed in favour of ForecastModel implementation.
}
