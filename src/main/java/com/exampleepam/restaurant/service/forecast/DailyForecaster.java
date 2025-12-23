package com.exampleepam.restaurant.service.forecast;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DailyForecaster {

    private static final int PAST_DAYS = 30;

    public ScaleData forecast(long id,
                              HistoryCollector.History history,
                              LocalDate today,
                              Map<YearMonth, Integer> monthForecastMap) {

        Map<LocalDate, Integer> dishDaily = history.dailyTotals.getOrDefault(id, Map.of());

        // Past window: today-30..today
        int pastSize = PAST_DAYS + 1;

        // Horizon: tomorrow..end of next month
        LocalDate startFuture = today.plusDays(1);
        LocalDate horizonEnd = YearMonth.from(today.plusMonths(1)).atEndOfMonth();
        int futureSize = (startFuture.isAfter(horizonEnd)) ? 0 : (int) (horizonEnd.toEpochDay() - startFuture.toEpochDay() + 1);

        List<String> labels = new ArrayList<>(pastSize + futureSize);
        List<Integer> actual = new ArrayList<>(pastSize + futureSize);
        List<Integer> forecast = new ArrayList<>(pastSize + futureSize);

        for (int i = -PAST_DAYS; i <= 0; i++) {
            LocalDate day = today.plusDays(i);
            labels.add(day.toString());
            actual.add(dishDaily.getOrDefault(day, 0));
            forecast.add(null);
        }

        // Precompute actual sums per month up to "today"
        Map<YearMonth, Integer> actualToDateByMonth = sumActualToDateByMonth(dishDaily, today);

        // Allocate for each month in horizon (could be current month remainder + full next month)
        LocalDate d = startFuture;
        while (!d.isAfter(horizonEnd)) {
            YearMonth ym = YearMonth.from(d);

            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();

            LocalDate allocStart = d; // starts at tomorrow for first month, then 1st for next month
            LocalDate allocEnd = monthEnd.isBefore(horizonEnd) ? monthEnd : horizonEnd;

            int monthPred = monthForecastMap.getOrDefault(ym, 0);
            int actualSoFar = actualToDateByMonth.getOrDefault(ym, 0);
            int remainingQty = Math.max(0, monthPred - actualSoFar);

            int days = (int) (allocEnd.toEpochDay() - allocStart.toEpochDay() + 1);
            allocateEvenly(allocStart, days, remainingQty, labels, actual, forecast);

            d = allocEnd.plusDays(1);
        }

        return new ScaleData(labels, actual, forecast);
    }

    private static Map<YearMonth, Integer> sumActualToDateByMonth(Map<LocalDate, Integer> dishDaily, LocalDate today) {
        Map<YearMonth, Integer> sums = new HashMap<>();
        for (Map.Entry<LocalDate, Integer> e : dishDaily.entrySet()) {
            LocalDate day = e.getKey();
            if (day == null || day.isAfter(today)) continue;

            int v = e.getValue() == null ? 0 : e.getValue();
            if (v == 0) continue;

            sums.merge(YearMonth.from(day), v, Integer::sum);
        }
        return sums;
    }

    /**
     * Even allocation:
     * base = total/days, first (total%days) days get +1.
     * Guarantees the sum equals total and values are non-negative.
     */
    private static void allocateEvenly(LocalDate startDay,
                                       int days,
                                       int total,
                                       List<String> labels,
                                       List<Integer> actual,
                                       List<Integer> forecast) {
        if (days <= 0) return;

        int base = total / days;
        int extra = total % days;

        for (int i = 0; i < days; i++) {
            LocalDate day = startDay.plusDays(i);
            int val = base + (i < extra ? 1 : 0);

            labels.add(day.toString());
            actual.add(null);
            forecast.add(val); // always >= 0
        }
    }
}
