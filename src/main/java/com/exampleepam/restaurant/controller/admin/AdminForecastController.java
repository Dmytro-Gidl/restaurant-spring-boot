package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.dto.forecast.DishForecastDto;
import com.exampleepam.restaurant.dto.forecast.SummaryForecastDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.service.ForecastService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Controller providing forecasting information for managers.
 */
@Controller
@RequestMapping("/admin/forecast")
public class AdminForecastController {

    private static final String FORECAST_PAGE = "forecast";
    private static final String FORECASTS_ATTR = "forecasts";
    private static final String FILTER_ATTR = "filter";
    private static final String TYPE_ATTR = "type";
    private static final String SUMMARY_ATTR = "summary";
    private static final int HISTORY_DAYS = 7;
    private static final int PAGE_SIZE = 6;

    private final ForecastService forecastService;

    @Autowired
    public AdminForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    public String showForecast(@RequestParam(value = "filter", required = false) String filter,
                               @RequestParam(value = "type", required = false) Category type,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               Model model) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<DishForecastDto> forecasts = forecastService.getDishForecasts(HISTORY_DAYS, filter, type, pageable);
        model.addAttribute(FORECASTS_ATTR, forecasts.getContent());
        model.addAttribute(SUMMARY_ATTR, buildSummary(forecasts.getContent()));
        model.addAttribute("page", forecasts);
        model.addAttribute(FILTER_ATTR, filter);
        model.addAttribute(TYPE_ATTR, type);
        model.addAttribute("categories", Category.values());
        return FORECAST_PAGE;
    }

    private SummaryForecastDto buildSummary(java.util.List<DishForecastDto> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            return null;
        }
        java.util.Map<String, java.util.List<String>> labels = forecasts.get(0).getLabels();
        java.util.Map<String, java.util.List<Integer>> totalActual = new java.util.HashMap<>();
        java.util.Map<String, java.util.List<Integer>> totalForecast = new java.util.HashMap<>();
        for (String scale : labels.keySet()) {
            int size = labels.get(scale).size();
            java.util.List<Integer> actual = new java.util.ArrayList<>(java.util.Collections.nCopies(size, 0));
            java.util.List<Integer> forecast = new java.util.ArrayList<>(java.util.Collections.nCopies(size, 0));
            boolean[] actualSeen = new boolean[size];
            boolean[] forecastSeen = new boolean[size];
            for (DishForecastDto dto : forecasts) {
                java.util.List<Integer> aList = dto.getActualData().get(scale);
                java.util.List<Integer> fList = dto.getForecastData().get(scale);
                for (int i = 0; i < size; i++) {
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
            for (int i = 0; i < size; i++) {
                if (!actualSeen[i]) actual.set(i, null);
                if (!forecastSeen[i]) forecast.set(i, null);
            }
            totalActual.put(scale, actual);
            totalForecast.put(scale, forecast);
        }
        return new SummaryForecastDto(labels, totalActual, totalForecast);
    }
}
