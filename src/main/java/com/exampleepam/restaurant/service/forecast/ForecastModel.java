package com.exampleepam.restaurant.service.forecast;

import java.util.List;

/**
 * Generic interface for demand-forecasting models.
 */
public interface ForecastModel {

    /** @return short identifier used when selecting the model */
    String getName();

    /**
     * Produces forecasts for the supplied historical series.
     *
     * @param history ordered list of past observations
     * @param periods number of future periods to predict
     * @return result containing forecasts and model diagnostics
     */
    ForecastResult forecast(List<Integer> history, int periods);
}

