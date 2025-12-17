package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.controller.BaseController;
import com.exampleepam.restaurant.dto.forecast.IngredientForecastDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.service.IngredientForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller providing ingredient forecasting information for managers.
 */
@Controller
@RequestMapping("/admin/ingredient-forecast")
public class AdminIngredientForecastController extends BaseController {

    private static final String FORECAST_PAGE = "ingredient-forecast";
    private static final String FORECASTS_ATTR = "forecasts";
    private static final String FILTER_ATTR = "filter";
    private static final int HISTORY_DAYS = 7;
    private static final int PAGE_SIZE = 6;

    private final IngredientForecastService forecastService;

    @Autowired
    public AdminIngredientForecastController(IngredientForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    public String showForecast(@RequestParam(value = "filter", required = false) String filter,
                               @RequestParam(value = "type", required = false) Category type,
                               @RequestParam(value = "model", defaultValue = "holt") String modelName,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               Model model) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<IngredientForecastDto> forecasts = forecastService.getIngredientForecasts(HISTORY_DAYS, filter, type, modelName, pageable);
        model.addAttribute(FORECASTS_ATTR, forecasts.getContent());
        model.addAttribute("page", forecasts);
        model.addAttribute(FILTER_ATTR, filter);
        model.addAttribute("type", type);
        model.addAttribute("model", modelName);
        model.addAttribute("models", java.util.List.of("holt","arima"));
        model.addAttribute("categories", Category.values());
        return FORECAST_PAGE;
    }

    @GetMapping("/details")
    @ResponseBody
    public java.util.List<com.exampleepam.restaurant.entity.IngredientForecast> details(@RequestParam("ingredientId") long id) {
        return forecastService.getDetails(id);
    }
}