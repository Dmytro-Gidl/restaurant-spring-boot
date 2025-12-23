package com.exampleepam.restaurant.service.forecast;

import java.util.List;

/**
 * Holds forecasted values and diagnostic information for a model run.
 */
public class ForecastResult {
    private final List<Double> forecasts;
    private final double alpha;
    private final double beta;
    private final double gamma;
    private final double mape;
    private final double rmse;
    private final List<Double> lower;
    private final List<Double> upper;

    public ForecastResult(List<Double> forecasts, double alpha, double beta, double gamma,
                          double mape, double rmse, List<Double> lower, List<Double> upper) {
        this.forecasts = forecasts;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.mape = mape;
        this.rmse = rmse;
        this.lower = lower;
        this.upper = upper;
    }

    public List<Double> getForecasts() { return forecasts; }
    public double getAlpha() { return alpha; }
    public double getBeta() { return beta; }
    public double getGamma() { return gamma; }
    public double getMape() { return mape; }
    public double getRmse() { return rmse; }
    public List<Double> getLower() { return lower; }
    public List<Double> getUpper() { return upper; }
}
