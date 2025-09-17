package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.entity.paging.Paged;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientForecastServiceTest {

    private IngredientForecastService ingredientForecastService;

    @BeforeEach
    void setUp() {
        ingredientForecastService = new IngredientForecastService();
    }

    @Test
    void getIngredientForecastsReturnsAllElementsWhenUnpaged() {
        List<String> forecasts = List.of("Onion", "Milk", "Bread");

        Paged<String> result = ingredientForecastService.getIngredientForecasts(
                forecasts, Pageable.unpaged());

        Page<String> page = result.getPage();

        assertThat(page.getContent()).containsExactlyElementsOf(forecasts);
        assertThat(result.getPaging().getPageNumber()).isEqualTo(1);
        assertThat(result.getPaging().getPageSize()).isEqualTo(forecasts.size());
        assertThat(result.getPaging().getTotalPages()).isEqualTo(1);
    }

    @Test
    void getIngredientForecastsSlicesContentAccordingToPageable() {
        List<Integer> forecasts = List.of(1, 2, 3, 4, 5);
        PageRequest pageable = PageRequest.of(1, 2);

        Paged<Integer> result = ingredientForecastService.getIngredientForecasts(
                forecasts, pageable);

        Page<Integer> page = result.getPage();

        assertThat(page.getContent()).containsExactly(3, 4);
        assertThat(result.getPaging().getPageNumber()).isEqualTo(2);
        assertThat(result.getPaging().getPageSize()).isEqualTo(2);
        assertThat(result.getPaging().getTotalPages()).isEqualTo(3);
    }

    @Test
    void getIngredientForecastsTreatsNullPageableAsUnpaged() {
        List<String> forecasts = List.of("Tea", "Coffee");

        Paged<String> result = ingredientForecastService.getIngredientForecasts(forecasts, null);

        assertThat(result.getPage().getContent()).containsExactlyElementsOf(forecasts);
        assertThat(result.getPaging().getTotalPages()).isEqualTo(1);
        assertThat(result.getPaging().getPageNumber()).isEqualTo(1);
    }

    @Test
    void getIngredientForecastsHandlesEmptyCollection() {
        Paged<String> result = ingredientForecastService.getIngredientForecasts(List.of(), Pageable.unpaged());

        assertThat(result.getPage().getContent()).isEmpty();
        assertThat(result.getPaging().getTotalPages()).isZero();
        assertThat(result.getPaging().getPageNumber()).isZero();
        assertThat(result.getPaging().isNextEnabled()).isFalse();
        assertThat(result.getPaging().isPrevEnabled()).isFalse();
    }

    @Test
    void getIngredientForecastsHandlesNullCollection() {
        Paged<String> result = ingredientForecastService.getIngredientForecasts(null, Pageable.unpaged());

        assertThat(result.getPage().getContent()).isEmpty();
        assertThat(result.getPaging().getTotalPages()).isZero();
    }
}
