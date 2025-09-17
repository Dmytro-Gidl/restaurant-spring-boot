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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class IngredientForecastServiceTest {

//    @Test
//    void aggregatesMonthlyActualUsage() {
//        DishForecastService dishForecastService = Mockito.mock(DishForecastService.class);
//        DishRepository dishRepository = Mockito.mock(DishRepository.class);
//        IngredientRepository ingredientRepository = Mockito.mock(IngredientRepository.class);
//        IngredientForecastRepository forecastRepository = Mockito.mock(IngredientForecastRepository.class);
//
//        Ingredient ing = new Ingredient();
//        ing.setId(1L);
//        ing.setName("Cheese");
//        ing.setUnit(MeasureUnit.GRAM);
//
//        Dish dish = new Dish();
//        dish.setId(10L);
//        DishIngredient di = new DishIngredient();
//        di.setDish(dish);
//        di.setIngredient(ing);
//        di.setQuantity(2);
//        dish.getIngredients().add(di);
//
//        Mockito.when(dishRepository.findAll()).thenReturn(List.of(dish));
//        Mockito.when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ing));
//
//        Map<String, List<String>> labels = Map.of("monthly", List.of("2023-01", "2023-02"));
//        Map<String, List<Integer>> actual = new HashMap<>();
//        actual.put("monthly", new ArrayList<>(Arrays.asList(0, 5)));
//        Map<String, List<Integer>> forecast = Map.of("monthly", Arrays.asList(7, 8));
//        DishForecastDto df = new DishForecastDto(10L, "Dish", null, labels, actual, forecast, false, false);
//        Mockito.when(dishForecastService.getDishForecasts(Mockito.anyInt(), Mockito.isNull(), Mockito.isNull(), Mockito.anyString(), Mockito.eq(Pageable.unpaged())))
//                .thenReturn(new PageImpl<>(List.of(df)));
//
//        IngredientForecastService service = new IngredientForecastService(dishForecastService, dishRepository, ingredientRepository, forecastRepository);
//        Page<IngredientForecastDto> page = service.getIngredientForecasts(30, null, null, "holt", Pageable.unpaged());
//        IngredientForecastDto dto = page.getContent().get(0);
//        List<Integer> monthly = dto.getActualData().get("monthly");
//        assertEquals(0, monthly.get(0));
//        assertEquals(10, monthly.get(1));
//        assertTrue(dto.isSinglePoint());
//        assertFalse(dto.isNoData());
//    }
}
