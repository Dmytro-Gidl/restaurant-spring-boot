package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that produces demand forecasts for dishes.  Forecasting is based on
 * simple exponential smoothing which gives more weight to recent orders while
 * still considering older history.  The resulting predictions are rounded to
 * integers since fractional portions of dishes cannot be prepared.
 */
@Service
public class ForecastService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;

    @Autowired
    public ForecastService(OrderRepository orderRepository, DishRepository dishRepository) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
    }

    /**
     * Builds forecast data for dishes across hourly, daily and monthly ranges.
     *
     * @param historyDays number of previous days to analyse for hourly trends
     * @param filter      optional dish name filter
     * @param type        optional dish category filter
     * @return page of forecast DTOs, possibly empty
     */
    public Page<DishForecastDto> getDishForecasts(int historyDays, String filter, Category type, Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusYears(2).atStartOfDay();
        List<Order> orders = orderRepository.findByStatusAndCreationDateTimeAfter(Status.COMPLETED, start);

        Map<Long, Map<LocalDate, int[]>> hourlyTotals = new HashMap<>();
        Map<Long, Map<LocalDate, Integer>> dailyTotals = new HashMap<>();
        Map<Long, Map<YearMonth, Integer>> monthlyTotals = new HashMap<>();

        for (Order order : orders) {
            LocalDateTime dateTime = order.getCreationDateTime();
            LocalDate date = dateTime.toLocalDate();
            int hour = dateTime.getHour();
            YearMonth ym = YearMonth.from(dateTime);
            for (OrderItem item : order.getOrderItems()) {
                long dishId = item.getDish().getId();
                int qty = item.getQuantity();

                hourlyTotals.computeIfAbsent(dishId, k -> new HashMap<>())
                        .computeIfAbsent(date, d -> new int[24])[hour] += qty;

                dailyTotals.computeIfAbsent(dishId, k -> new HashMap<>())
                        .merge(date, qty, Integer::sum);
                monthlyTotals.computeIfAbsent(dishId, k -> new HashMap<>())
                        .merge(ym, qty, Integer::sum);
            }
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
        Page<Dish> dishes;
        if ((filter == null || filter.isBlank()) && type == null) {
            dishes = dishRepository.findAllByArchivedFalse(sortedPageable);
        } else if ((filter == null || filter.isBlank())) {
            dishes = dishRepository.findByCategoryAndArchivedFalse(type, sortedPageable);
        } else if (type == null) {
            dishes = dishRepository.findByNameContainingIgnoreCaseAndArchivedFalse(filter, sortedPageable);
        } else {
            dishes = dishRepository.findByNameContainingIgnoreCaseAndCategoryAndArchivedFalse(filter, type, sortedPageable);
        }

        List<DishForecastDto> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        final double alpha = 0.5; // smoothing factor
        for (Dish dish : dishes.getContent()) {
            long id = dish.getId();

            Map<String, List<String>> labelsMap = new HashMap<>();
            Map<String, List<Integer>> actualMap = new HashMap<>();
            Map<String, List<Integer>> forecastMap = new HashMap<>();

            // Hourly (7 days before and after)
            Map<LocalDate, int[]> dishHours = hourlyTotals.getOrDefault(id, Map.of());
            List<int[]> pastArrays = dishHours.entrySet().stream()
                    .filter(e -> e.getKey().isBefore(today) && !e.getKey().isBefore(today.minusDays(historyDays)))
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            double[] hourForecastValues = smoothHourly(pastArrays, alpha);
            List<String> hLabels = new ArrayList<>();
            List<Integer> hActual = new ArrayList<>();
            List<Integer> hForecast = new ArrayList<>();
            DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("MM-dd HH:00");
            LocalDateTime startHour = today.minusDays(7).atStartOfDay();
            for (int i = 0; i < 24 * 15; i++) { // 7 days before + today + 7 days after
                LocalDateTime dt = startHour.plusHours(i);
                hLabels.add(dt.format(hourFmt));
                int hour = dt.getHour();
                if (dt.isBefore(now)) {
                    int[] arr = dishHours.getOrDefault(dt.toLocalDate(), new int[24]);
                    hActual.add(arr[hour]);
                    hForecast.add(null);
                } else {
                    int predicted = (int) Math.max(0, Math.round(hourForecastValues[hour]));
                    hActual.add(null);
                    hForecast.add(predicted);
                }
            }
            labelsMap.put("hourly", hLabels);
            actualMap.put("hourly", hActual);
            forecastMap.put("hourly", hForecast);

            // Daily (30 days before and after)
            Map<LocalDate, Integer> dishDaily = dailyTotals.getOrDefault(id, Map.of());
            LocalDate startDay = today.minusDays(30);
            List<String> dLabels = new ArrayList<>();
            List<Integer> dActual = new ArrayList<>();
            List<Integer> dForecast = new ArrayList<>();
            List<Integer> historyDaysList = new ArrayList<>();
            for (int i = 0; i <= 30; i++) {
                LocalDate day = startDay.plusDays(i);
                int val = dishDaily.getOrDefault(day, 0);
                dLabels.add(day.toString());
                dActual.add(val);
                dForecast.add(null);
                if (day.isBefore(today)) {
                    historyDaysList.add(val);
                }
            }
            double dailySmoothed = exponentialSmooth(historyDaysList, alpha);
            int dailyPrediction = (int) Math.max(0, Math.round(dailySmoothed));
            for (int i = 1; i <= 30; i++) {
                LocalDate day = today.plusDays(i);
                dLabels.add(day.toString());
                dActual.add(null);
                dForecast.add(dailyPrediction);
            }
            labelsMap.put("daily", dLabels);
            actualMap.put("daily", dActual);
            forecastMap.put("daily", dForecast);

            // Monthly (2 years before and after)
            Map<YearMonth, Integer> dishMonthly = monthlyTotals.getOrDefault(id, Map.of());
            YearMonth currentMonth = YearMonth.now();
            YearMonth startMonth = currentMonth.minusMonths(24);
            List<String> mLabels = new ArrayList<>();
            List<Integer> mActual = new ArrayList<>();
            List<Integer> mForecast = new ArrayList<>();
            List<Integer> historyMonths = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                YearMonth ym = startMonth.plusMonths(i);
                int val = dishMonthly.getOrDefault(ym, 0);
                mLabels.add(ym.toString());
                mActual.add(val);
                mForecast.add(null);
                historyMonths.add(val);
            }
            int currentVal = dishMonthly.getOrDefault(currentMonth, 0);
            mLabels.add(currentMonth.toString());
            mActual.add(currentVal);
            mForecast.add(null);
            historyMonths.add(currentVal);
            double monthSmoothed = exponentialSmooth(historyMonths, alpha);
            int monthPrediction = (int) Math.max(0, Math.round(monthSmoothed));
            for (int i = 1; i <= 24; i++) {
                YearMonth ym = currentMonth.plusMonths(i);
                mLabels.add(ym.toString());
                mActual.add(null);
                mForecast.add(monthPrediction);
            }
            labelsMap.put("monthly", mLabels);
            actualMap.put("monthly", mActual);
            forecastMap.put("monthly", mForecast);

            result.add(new DishForecastDto(id, dish.getName(), dish.getImagePath(), labelsMap, actualMap, forecastMap));
        }

        return new PageImpl<>(result, pageable, dishes.getTotalElements());
    }

    private double[] smoothHourly(List<int[]> pastArrays, double alpha) {
        double[] smooth = new double[24];
        if (pastArrays.isEmpty()) {
            return smooth;
        }
        int[] first = pastArrays.get(0);
        for (int h = 0; h < 24; h++) {
            smooth[h] = first[h];
        }
        for (int i = 1; i < pastArrays.size(); i++) {
            int[] arr = pastArrays.get(i);
            for (int h = 0; h < 24; h++) {
                smooth[h] = alpha * arr[h] + (1 - alpha) * smooth[h];
            }
        }
        return smooth;
    }

    private double exponentialSmooth(List<Integer> values, double alpha) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double s = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            s = alpha * values.get(i) + (1 - alpha) * s;
        }
        return s;
    }
}
