package com.exampleepam.restaurant.service.forecast;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class DailyForecaster {

    public ScaleData forecast(long id,
                              HistoryCollector.History history,
                              LocalDate today,
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
}
