package com.exampleepam.restaurant.dto.forecast;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DTO representing forecast information for a dish.
 */
@Getter
@Setter
@AllArgsConstructor
public class DishForecastDto {
    private long id;
    private String name;
    private String imagePath;
    private Map<String, List<String>> labels;
    private Map<String, List<Integer>> actualData;
    private Map<String, List<Integer>> forecastData;
    private boolean singlePoint;
    private boolean noData;
}
