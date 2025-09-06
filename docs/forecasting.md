# Forecasting Methodology

We rely on a small, transparent pipeline to look a few weeks ahead at dish demand and ingredient usage.

The application supports three lightweight models:

- **Holt‑Winters (default):** smooths gradual trends and is used when no model is specified.
- **ARIMA(1,0,0):** reacts to month‑to‑month swings around a stable mean.
- **auto‑ARIMA:** automatically chooses between a simple mean and an AR(1) based on the data.

If trimming leaves only a single non‑zero month, all three models simply repeat that value until more history exists.

For a plain-language overview of the forecasting options, visit the in-app <a href="/admin/dish-forecast/about">help page</a>. It explains how Holt‑Winters smooths gradual changes, ARIMA uses the last month to guess the next, and auto‑ARIMA automatically picks the simpler of the two based on recent data.

1. **Monthly baseline.** Up to two years of orders are grouped by month. Leading months that contain only zeros are trimmed so dormant periods do not dominate model fitting. If a single non‑zero month remains after trimming, the system issues a naive forecast by repeating that value and flags the result in the details panel. Three models can then be applied:
   - **Holt‑Winters:** triple exponential smoothing capturing level and trend. Works well for gradually changing demand and is the current default.
   - **ARIMA(1,0,0):** a simple autoregressive model assuming stationarity. Useful when demand fluctuates around a mean.
   - **auto‑ARIMA:** selects among mean and AR(1) structures via AIC. Provides a lightweight automatic alternative when parameters are unknown.
2. **Daily breakdown with reconciliation.** Monthly forecasts are converted into daily values. For future days, the remainder of each month is distributed evenly and then reconciled so that the daily sum equals the monthly prediction exactly.
3. **Hourly breakdown with reconciliation.** Recent hourly order patterns provide weights that disaggregate each day into 24 buckets. A reconciliation step adjusts the final hour to ensure each day's hourly total equals its daily forecast.

Ingredient forecasts multiply dish forecasts by ingredient usage. Because every ingredient has a fixed base unit (pieces or grams), aggregates remain consistent.

Accuracy statistics and chosen parameters for each model are available from the admin interface via expandable “Details” panels, supporting the academic requirement for transparency. When evaluating model performance, a simple k‑fold cross‑validation runs each model on contiguous segments of the monthly history. The resulting mean absolute percentage error (MAPE) divides by the count of non‑zero observations, and metrics are reported only when at least two such months exist.

Sparse data often yields identical forecasts across models. For example, with only two completed orders in one month, the history collapses to a single point so every model repeats that value. Gathering additional completed orders is required for differentiation.

### Effect of additional history
With **steady growth** over several months, Holt‑Winters projects the rising trend, ARIMA(1,0,0) lags slightly behind it, and auto‑ARIMA may switch from a mean to AR(1) model as evidence of the trend accumulates. In a **mean‑reverting** pattern that swings above and below an average, ARIMA mirrors the oscillations, Holt‑Winters dampens them, and auto‑ARIMA chooses the option with the lowest AIC. When orders are **sparse and sporadic**, all three models output the same flat line because the trimmed history contains only one meaningful point.

Sample datasets illustrating steady growth, mean reversion and sparse histories are available in [forecast-scenarios.sql](forecast-scenarios.sql). Monthly histories collected by `HistoryCollector` can also be exported via `History.exportMonthlyCsv` and analysed externally. The companion script [forecast_benchmark.py](forecast_benchmark.py) demonstrates running a statsmodels ARIMA on the exported CSV and comparing MAPE/RMSE against the in-app models.

Example comparison on the steady-growth scenario (`5,10,15,20`) produced the following one-month-ahead errors:

| Model | MAPE | RMSE |
|-------|------|------|
| Holt-Winters (app) | 0.00 | 0.00 |
| statsmodels ARIMA(1,0,0) | 6.25 | 1.25 |

### Data requirements
- At least a few months of **completed** orders are needed; pending orders are ignored until completion.
- Ingredients must declare a base unit (pieces or grams) so dish forecasts can be converted into ingredient demand.

### Model assumptions
- Holt–Winters assumes a linear trend without strong seasonality; the automatic ARIMA model provides a mean or AR(1) alternative.
- Forecasts are rounded to whole units because fractional dishes cannot be prepared.

### Forecast refresh
- Forecasts are recomputed nightly by a scheduler and immediately after an order is marked `COMPLETED`.
- All registered models are run so that administrators can compare accuracy.

### Troubleshooting
- **No forecasts**: there may be insufficient completed orders or the scheduler has not run yet.
- **Stale results**: ensure the application was running when the scheduler triggered or use the admin page after completing an order to force a refresh.
- **Missing ingredients**: dishes without ingredients simply yield no ingredient forecasts until components are added.

**References**
- C. C. Holt. *Forecasting seasonals and trends by exponentially weighted moving averages*. ONR Research Memorandum, 1957.
- G. E. P. Box and G. M. Jenkins. *Time Series Analysis: Forecasting and Control*. Holden-Day, 1976.
