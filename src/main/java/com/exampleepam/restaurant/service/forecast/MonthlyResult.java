package com.exampleepam.restaurant.service.forecast;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import com.exampleepam.restaurant.service.forecast.ForecastResult;

public record MonthlyResult(ScaleData scale,
                            Map<YearMonth, Integer> monthForecasts,
                            List<Integer> modelHistory,
                            ForecastResult result,
                            boolean singlePoint,
                            boolean noData,
                            boolean emptyForecast) {}
