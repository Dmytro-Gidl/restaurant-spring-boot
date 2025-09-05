package com.exampleepam.restaurant.dto.forecast;

import com.exampleepam.restaurant.entity.MeasureUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DTO representing forecast information for an ingredient.
 */
@Getter
@Setter
@AllArgsConstructor
public class IngredientForecastDto {
    private long id;
    private String name;
    private MeasureUnit unit;
    private Map<String, List<String>> labels;
    private Map<String, List<Integer>> actualData;
    private Map<String, List<Integer>> forecastData;
}
