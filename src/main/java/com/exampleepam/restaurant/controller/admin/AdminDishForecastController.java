package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.controller.BaseController;
import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.service.ForecastSummaryService;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.service.DishForecastService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Controller providing forecasting information for managers.
 */
@Controller
@RequestMapping("/admin/dish-forecast")
public class AdminDishForecastController extends BaseController {

    private static final String FORECAST_PAGE = "dish-forecast";
    private static final String FORECASTS_ATTR = "forecasts";
    private static final String FILTER_ATTR = "filter";
    private static final String TYPE_ATTR = "type";
    private static final String SUMMARY_ATTR = "summary";
    private static final int HISTORY_DAYS = 7;
    private static final int PAGE_SIZE = 6;

    private final DishForecastService forecastService;
    private final ForecastSummaryService summaryService;

    @Autowired
    public AdminDishForecastController(DishForecastService forecastService,
                                       ForecastSummaryService summaryService) {
        this.forecastService = forecastService;
        this.summaryService = summaryService;
    }

    @GetMapping
    public String showForecast(@RequestParam(value = "filter", required = false) String filter,
                               @RequestParam(value = "type", required = false) Category type,
                               @RequestParam(value = "model", defaultValue = "holt") String modelName,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               Model model) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<DishForecastDto> forecasts = forecastService.getDishForecasts(HISTORY_DAYS, filter, type, modelName, pageable);
        model.addAttribute(FORECASTS_ATTR, forecasts.getContent());
        model.addAttribute(SUMMARY_ATTR, summaryService.summarize(forecasts.getContent()));
        model.addAttribute("page", forecasts);
        model.addAttribute(FILTER_ATTR, filter);
        model.addAttribute(TYPE_ATTR, type);
        model.addAttribute("model", modelName);
        model.addAttribute("models", java.util.List.of("holt","arima"));
        model.addAttribute("metrics", forecastService.getModelMetrics());
        model.addAttribute("categories", Category.values());
        return FORECAST_PAGE;
    }

    @GetMapping("/details")
    @ResponseBody
    public DishForecastService.ForecastDetails details(@RequestParam("model") String model,
                                                       @RequestParam("dishId") long dishId) {
        return forecastService.getDetails(model, dishId);
    }

    @GetMapping("/about")
    public String about() {
        return "dish-forecast-info";
    }
}
