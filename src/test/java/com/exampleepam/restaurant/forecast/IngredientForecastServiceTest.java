package com.exampleepam.restaurant.forecast;

import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.dto.forecast.IngredientForecastDto;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.DishIngredient;
import com.exampleepam.restaurant.entity.Ingredient;
import com.exampleepam.restaurant.entity.MeasureUnit;
import com.exampleepam.restaurant.repository.DishRepository;
import com.exampleepam.restaurant.repository.IngredientForecastRepository;
import com.exampleepam.restaurant.repository.IngredientRepository;
import com.exampleepam.restaurant.service.DishForecastService;
import com.exampleepam.restaurant.service.IngredientForecastService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class IngredientForecastServiceTest {

    private static final long INGREDIENT_ID = 1L;
    private static final long DISH_ID = 10L;
    private static final String INGREDIENT_NAME = "Cheese";
    private static final String DISH_NAME = "Dish";
    private static final int INGREDIENT_QUANTITY = 2;
    private static final int HISTORY_DAYS = 30;
    private static final int MONTH_ZERO = 0;
    private static final int MONTH_VALUE = 5;
    private static final int FORECAST_ONE = 7;
    private static final int FORECAST_TWO = 8;
    private static final String MODEL_NAME = "holt";
    private static final String MONTH_ONE = "2023-01";
    private static final String MONTH_TWO = "2023-02";
    private static final String MONTHLY_SCALE = "monthly";

    @Test
    void aggregatesMonthlyActualUsage() {
        DishForecastService dishForecastService = Mockito.mock(DishForecastService.class);
        DishRepository dishRepository = Mockito.mock(DishRepository.class);
        IngredientRepository ingredientRepository = Mockito.mock(IngredientRepository.class);
        IngredientForecastRepository forecastRepository = Mockito.mock(IngredientForecastRepository.class);

        Ingredient ing = new Ingredient();
        ing.setId(INGREDIENT_ID);
        ing.setName(INGREDIENT_NAME);
        ing.setUnit(MeasureUnit.GRAMS);

        Dish dish = new Dish();
        dish.setId(DISH_ID);
        dish.setName(DISH_NAME);
        DishIngredient di = new DishIngredient();
        di.setDish(dish);
        di.setIngredient(ing);
        di.setQuantity(INGREDIENT_QUANTITY);
        dish.getIngredients().add(di);

        Mockito.when(dishRepository.findAll()).thenReturn(List.of(dish));
        Mockito.when(ingredientRepository.findById(INGREDIENT_ID)).thenReturn(Optional.of(ing));

        Map<String, List<String>> labels = Map.of(MONTHLY_SCALE, List.of(MONTH_ONE, MONTH_TWO));
        Map<String, List<Integer>> actual = new HashMap<>();
        actual.put(MONTHLY_SCALE, new ArrayList<>(List.of(MONTH_ZERO, MONTH_VALUE)));
        Map<String, List<Integer>> forecast = Map.of(MONTHLY_SCALE, List.of(FORECAST_ONE, FORECAST_TWO));
        DishForecastDto df = new DishForecastDto(DISH_ID, DISH_NAME, null, labels, actual, forecast, false, false, false);
        Mockito.when(dishForecastService.getDishForecasts(
                Mockito.anyInt(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.anyString(),
                Mockito.eq(Pageable.unpaged()),
                Mockito.eq(false)))
                .thenReturn(new PageImpl<>(List.of(df)));

        IngredientForecastService service = new IngredientForecastService(
                dishForecastService, dishRepository, ingredientRepository, forecastRepository);
        Page<IngredientForecastDto> page = service.getIngredientForecasts(
                HISTORY_DAYS, null, null, MODEL_NAME, Pageable.unpaged());
        IngredientForecastDto dto = page.getContent().get(0);
        List<Integer> monthly = dto.getActualData().get(MONTHLY_SCALE);
        List<Integer> monthlyForecast = dto.getForecastData().get(MONTHLY_SCALE);

        assertEquals(MONTH_ZERO, monthly.get(0));
        assertEquals(MONTH_VALUE * INGREDIENT_QUANTITY, monthly.get(1));
        assertEquals(FORECAST_ONE * INGREDIENT_QUANTITY, monthlyForecast.get(0));
        assertEquals(FORECAST_TWO * INGREDIENT_QUANTITY, monthlyForecast.get(1));
        assertTrue(dto.isSinglePoint());
        assertFalse(dto.isNoData());
    }
}
