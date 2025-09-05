package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.dto.forecast.SummaryForecastDto;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Aggregates dish forecasts into a single summary object for the charts.
 */
@Service
public class ForecastSummaryService {

    public SummaryForecastDto summarize(List<DishForecastDto> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            return null;
        }
        Map<String, List<String>> labels = new HashMap<>();
        Map<String, List<Integer>> totalActual = new HashMap<>();
        Map<String, List<Integer>> totalForecast = new HashMap<>();
        Set<String> scales = forecasts.get(0).getLabels().keySet();
        for (String scale : scales) {
            int minSize = forecasts.stream()
                    .mapToInt(f -> f.getLabels().get(scale).size())
                    .min().orElse(0);
            List<String> baseLabels = forecasts.get(0).getLabels().get(scale).subList(0, minSize);
            labels.put(scale, new ArrayList<>(baseLabels));
            List<Integer> actual = new ArrayList<>(Collections.nCopies(minSize, 0));
            List<Integer> forecast = new ArrayList<>(Collections.nCopies(minSize, 0));
            boolean[] actualSeen = new boolean[minSize];
            boolean[] forecastSeen = new boolean[minSize];
            for (DishForecastDto dto : forecasts) {
                List<Integer> aList = dto.getActualData().get(scale).subList(0, minSize);
                List<Integer> fList = dto.getForecastData().get(scale).subList(0, minSize);
                for (int i = 0; i < minSize; i++) {
                    Integer aVal = aList.get(i);
                    if (aVal != null) {
                        actual.set(i, actual.get(i) + aVal);
                        actualSeen[i] = true;
                    }
                    Integer fVal = fList.get(i);
                    if (fVal != null) {
                        forecast.set(i, forecast.get(i) + fVal);
                        forecastSeen[i] = true;
                    }
                }
            }
            for (int i = 0; i < minSize; i++) {
                if (!actualSeen[i]) actual.set(i, null);
                if (!forecastSeen[i]) forecast.set(i, null);
            }
            totalActual.put(scale, actual);
            totalForecast.put(scale, forecast);
        }
        return new SummaryForecastDto(labels, totalActual, totalForecast);
    }
}
