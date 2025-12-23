package com.exampleepam.restaurant.service.forecast;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

@Component
public class HourlyForecaster {

    private static final int HOURS = 24;
    private static final int PAST_DAYS = 7;
    private static final int TOTAL_DAYS = 15;
    private static final int TOTAL_HOURS = HOURS * TOTAL_DAYS;

    private static final int[] ZERO_DAY = new int[HOURS];
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("MM-dd HH:00");

    public ScaleData forecast(long id,
                              HistoryCollector.History history,
                              LocalDate today,
                              LocalDateTime now,
                              ScaleData daily,
                              int historyDays) {

        Map<LocalDate, int[]> dishHours = history.hourlyTotals.getOrDefault(id, Map.of());

        // 1) hour weights from past days
        double[] hourWeights = computeHourWeights(dishHours, today, historyDays);
        int[] weightOrder = orderByWeightDesc(hourWeights);

        // 2) daily forecast lookup by date (O(1))
        Map<LocalDate, Integer> dailyPredByDate = buildDailyPredMap(daily);

        // 3) build hourly timeline
        Map<LocalDate, int[]> futureAlloc = new HashMap<>();
        Map<LocalDate, List<Integer>> forecastIndicesByDay = new HashMap<>();

        List<String> labels = new ArrayList<>(TOTAL_HOURS);
        List<Integer> actual = new ArrayList<>(TOTAL_HOURS);
        List<Integer> forecast = new ArrayList<>(TOTAL_HOURS);

        LocalDateTime startHour = today.minusDays(PAST_DAYS).atStartOfDay();

        for (int i = 0; i < TOTAL_HOURS; i++) {
            LocalDateTime dt = startHour.plusHours(i);
            labels.add(dt.format(HOUR_FMT));

            int hour = dt.getHour();
            LocalDate date = dt.toLocalDate();

            if (dt.isBefore(now)) {
                int[] arr = dishHours.get(date);
                if (arr == null) arr = ZERO_DAY;
                actual.add(arr[hour]);
                forecast.add(null);
            } else {
                int dayPred = dailyPredByDate.getOrDefault(date, 0);
                int[] alloc = futureAlloc.computeIfAbsent(date, d -> distribute(dayPred, hourWeights, weightOrder));

                actual.add(null);
                forecast.add(alloc[hour]);

                forecastIndicesByDay.computeIfAbsent(date, d -> new ArrayList<>()).add(i);
            }
        }

        // 4) reconcile so each day sum matches daily forecast (no year collision, no negatives)
        reconcileHourlyToDaily(dailyPredByDate, forecast, forecastIndicesByDay);

        return new ScaleData(labels, actual, forecast);
    }

    private static double[] computeHourWeights(Map<LocalDate, int[]> dishHours, LocalDate today, int historyDays) {
        double[] w = new double[HOURS];
        double total = 0.0;

        LocalDate from = today.minusDays(Math.max(1, historyDays));
        for (Map.Entry<LocalDate, int[]> e : dishHours.entrySet()) {
            LocalDate d = e.getKey();
            if (!d.isBefore(today) || d.isBefore(from)) continue;

            int[] arr = e.getValue();
            if (arr == null || arr.length < HOURS) continue;

            for (int h = 0; h < HOURS; h++) {
                int v = arr[h];
                if (v != 0) {
                    w[h] += v;
                    total += v;
                }
            }
        }

        if (total == 0.0) {
            Arrays.fill(w, 1.0 / HOURS);
        } else {
            for (int h = 0; h < HOURS; h++) w[h] /= total;
        }
        return w;
    }

    private static Map<LocalDate, Integer> buildDailyPredMap(ScaleData daily) {
        Map<LocalDate, Integer> map = new HashMap<>();
        if (daily == null || daily.labels() == null || daily.forecast() == null) return map;

        int len = Math.min(daily.labels().size(), daily.forecast().size());
        for (int i = 0; i < len; i++) {
            String label = daily.labels().get(i);
            Integer v = daily.forecast().get(i);
            if (label == null) continue;

            // current codebase expects yyyy-MM-dd here (you already rely on it elsewhere)
            LocalDate d;
            try {
                d = LocalDate.parse(label);
            } catch (Exception ex) {
                continue;
            }

            map.put(d, v == null ? 0 : v);
        }
        return map;
    }

    private static void reconcileHourlyToDaily(Map<LocalDate, Integer> dailyPredByDate,
                                               List<Integer> hourlyForecast,
                                               Map<LocalDate, List<Integer>> forecastIndicesByDay) {

        // sum current hourly forecasts per day
        Map<LocalDate, Integer> sums = new HashMap<>();
        for (Map.Entry<LocalDate, List<Integer>> e : forecastIndicesByDay.entrySet()) {
            int s = 0;
            for (int idx : e.getValue()) {
                Integer v = hourlyForecast.get(idx);
                if (v != null) s += v;
            }
            sums.put(e.getKey(), s);
        }

        for (Map.Entry<LocalDate, Integer> e : dailyPredByDate.entrySet()) {
            LocalDate day = e.getKey();
            int dayPred = e.getValue() == null ? 0 : e.getValue();

            List<Integer> idxs = forecastIndicesByDay.get(day);
            if (idxs == null || idxs.isEmpty()) continue;

            int currentSum = sums.getOrDefault(day, 0);
            int diff = dayPred - currentSum;
            if (diff == 0) continue;

            // adjust from last hour backwards; never go below 0
            for (int k = idxs.size() - 1; k >= 0 && diff != 0; k--) {
                int idx = idxs.get(k);
                Integer cur = hourlyForecast.get(idx);
                if (cur == null) continue;

                if (diff > 0) {
                    hourlyForecast.set(idx, cur + diff);
                    diff = 0;
                } else { // diff < 0
                    int take = Math.min(cur, -diff);
                    hourlyForecast.set(idx, cur - take);
                    diff += take;
                }
            }
        }
    }

    private static int[] distribute(int total, double[] weights, int[] orderByWeight) {
        int[] arr = new int[HOURS];
        if (total <= 0) return arr;

        int remaining = total;
        for (int h = 0; h < HOURS; h++) {
            int v = (int) Math.floor(total * weights[h]);
            arr[h] = v;
            remaining -= v;
        }

        int idx = 0;
        while (remaining-- > 0) {
            arr[orderByWeight[idx % HOURS]]++;
            idx++;
        }
        return arr;
    }

    private static int[] orderByWeightDesc(double[] weights) {
        return IntStream.range(0, HOURS)
                .boxed()
                .sorted((a, b) -> Double.compare(weights[b], weights[a]))
                .mapToInt(Integer::intValue)
                .toArray();
    }
}
