package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.dto.forecast.SummaryForecastDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates dish forecasts into a single summary object for the charts.
 */
@Slf4j
@Service
public class ForecastSummaryService {

    public SummaryForecastDto summarize(List<DishForecastDto> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            return empty();
        }

        // filter nulls
        List<DishForecastDto> items = new ArrayList<>();
        for (DishForecastDto f : forecasts) if (f != null) items.add(f);
        if (items.isEmpty()) return empty();

        DishForecastDto first = items.get(0);
        Map<String, List<String>> firstLabels = safe(first.getLabels());
        if (firstLabels.isEmpty()) {
            log.debug("First forecast has no labels; producing empty summary");
            return empty();
        }

        log.debug("Summarizing {} dish forecasts", items.size());

        Map<String, List<String>> labels = new LinkedHashMap<>();
        Map<String, List<Integer>> totalActual = new LinkedHashMap<>();
        Map<String, List<Integer>> totalForecast = new LinkedHashMap<>();

        // Use scales from FIRST forecast (original behavior)
        for (String scale : firstLabels.keySet()) {
            // determine min length across all forecasts that have this scale
            int minSize = Integer.MAX_VALUE;
            for (DishForecastDto dto : items) {
                List<String> lab = getList(safe(dto.getLabels()), scale);
                if (lab == null) continue;
                minSize = Math.min(minSize, lab.size());
            }
            if (minSize == Integer.MAX_VALUE || minSize <= 0) {
                // scale not present anywhere with data; skip
                continue;
            }

            // base labels from FIRST forecast (original behavior)
            List<String> base = getList(firstLabels, scale);
            if (base == null || base.isEmpty()) continue;
            labels.put(scale, new ArrayList<>(base.subList(0, minSize)));

            // running sums & "seen" flags
            List<Integer> sumActual = new ArrayList<>(Collections.nCopies(minSize, 0));
            List<Integer> sumForecast = new ArrayList<>(Collections.nCopies(minSize, 0));
            boolean[] actualSeen = new boolean[minSize];
            boolean[] forecastSeen = new boolean[minSize];

            for (DishForecastDto dto : items) {
                List<Integer> aList = getList(safe(dto.getActualData()), scale);
                List<Integer> fList = getList(safe(dto.getForecastData()), scale);

                if (aList != null) {
                    int n = Math.min(minSize, aList.size());
                    for (int i = 0; i < n; i++) {
                        Integer v = aList.get(i);
                        if (v != null) {
                            sumActual.set(i, sumActual.get(i) + v);
                            actualSeen[i] = true;
                        }
                    }
                }
                if (fList != null) {
                    int n = Math.min(minSize, fList.size());
                    for (int i = 0; i < n; i++) {
                        Integer v = fList.get(i);
                        if (v != null) {
                            sumForecast.set(i, sumForecast.get(i) + v);
                            forecastSeen[i] = true;
                        }
                    }
                }
            }

            // mark positions with no observations as null
            for (int i = 0; i < minSize; i++) {
                if (!actualSeen[i]) sumActual.set(i, null);
                if (!forecastSeen[i]) sumForecast.set(i, null);
            }

            totalActual.put(scale, sumActual);
            totalForecast.put(scale, sumForecast);
        }

        if (labels.isEmpty()) {
            log.debug("No usable scales after alignment; producing empty summary");
            return empty();
        }

        log.debug("Summary scales: {}", labels.keySet());
        return new SummaryForecastDto(labels, totalActual, totalForecast);
    }

    // ---------- helpers ----------

    private static SummaryForecastDto empty() {
        return new SummaryForecastDto(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    private static <K, V> Map<K, V> safe(Map<K, V> m) {
        return m == null ? Collections.emptyMap() : m;
    }

    private static <T> List<T> getList(Map<String, List<T>> map, String key) {
        if (map == null) return null;
        List<T> v = map.get(key);
        return (v == null || v.isEmpty()) ? null : v;
    }
}
