package com.exampleepam.restaurant.service.forecast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class HourlyForecaster {

    public ScaleData forecast(long id,
                              HistoryCollector.History history,
                              LocalDate today,
                              LocalDateTime now,
                              ScaleData daily,
                              int historyDays) {
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
}
