"""Run external ARIMA forecasts for comparison.

Reads monthly history exported by HistoryCollector.exportMonthlyCsv and
prints a 12-step ARIMA(1,0,0) forecast along with MAPE/RMSE metrics.
Requires pandas and statsmodels.
"""
import sys
import pandas as pd
import statsmodels.api as sm


def main(path: str):
    data = pd.read_csv(path, parse_dates=["month"])
    series = data["quantity"].astype(float)
    model = sm.tsa.arima.ARIMA(series, order=(1, 0, 0)).fit()
    forecast = model.forecast(12)
    print("Forecast:", forecast.to_list())

    fitted = model.fittedvalues
    resid = series[1:] - fitted[1:]
    mape = (abs(resid) / series[1:]).replace([pd.NA, float("inf")], pd.NA).dropna().mean() * 100
    rmse = (resid.pow(2).mean()) ** 0.5
    print(f"MAPE: {mape:.2f} RMSE: {rmse:.2f}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python forecast_benchmark.py history.csv")
    else:
        main(sys.argv[1])
