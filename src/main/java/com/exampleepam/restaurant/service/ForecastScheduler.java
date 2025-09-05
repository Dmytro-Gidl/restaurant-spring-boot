package com.exampleepam.restaurant.service;

import com.exampleepam.restaurant.entity.Category;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.exampleepam.restaurant.service.forecast.ForecastModel;

@Component
public class ForecastScheduler {

    private final DishForecastService dishForecastService;
    private final IngredientForecastService ingredientForecastService;
    private final java.util.List<ForecastModel> models;

    public ForecastScheduler(DishForecastService dishForecastService,
                             IngredientForecastService ingredientForecastService,
                             java.util.List<ForecastModel> models) {
        this.dishForecastService = dishForecastService;
        this.ingredientForecastService = ingredientForecastService;
        this.models = models;
    }

    /** Refresh forecasts once per day. */
    @Scheduled(cron = "0 0 2 * * *")
    public void refreshForecasts() {
        for (ForecastModel m : models) {
            dishForecastService.getDishForecasts(7, null, null, m.getName(), org.springframework.data.domain.Pageable.unpaged());
            ingredientForecastService.getIngredientForecasts(7, null, null, m.getName(), org.springframework.data.domain.Pageable.unpaged());
        }
    }
}

