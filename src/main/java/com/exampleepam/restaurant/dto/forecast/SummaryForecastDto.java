package com.exampleepam.restaurant.dto.forecast;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DTO representing aggregated forecast information for currently displayed dishes.
 */
@Getter
@Setter
@AllArgsConstructor
public class SummaryForecastDto {
    private Map<String, List<String>> labels;
    private Map<String, List<Integer>> actualData;
    private Map<String, List<Integer>> forecastData;
}
