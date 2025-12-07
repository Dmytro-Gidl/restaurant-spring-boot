# Demo guide: forecasting vs recommendations

This guide walks professors through a reproducible end-to-end demo using the sample dataset in [`forecast-demo.sql`](forecast-demo.sql). It starts from an empty database and shows how each forecasting model behaves on distinct patterns, plus how the recommender responds to student reviews.

## How to load the data
1. Start the app connected to a throwaway database (e.g., `spring.profiles.active=dev`).
2. Apply the SQL in `docs/forecast-demo.sql` (e.g., via `psql`, `mysql`, or your IDE's console). It truncates core tables **and cached forecast rows** so you start clean, then seeds five dishes and users, and inserts **27 months (Jan 2024–Mar 2026)** of order history so Holt/ARIMA have a full seasonal cycle to train on. The app now keeps a **36‑month window** of history in memory and collects the last **three years** of completed orders, ensuring two complete years stay in view for the Holt seasonal period. Holt's seasonal period is set to **6 months** for the demo so the limited in-window history still yields non-flat projections.

## What to demonstrate

### Daily and hourly validity checks
* Daily forecasts reuse the current and next month’s predictions, spreading them across days between today and the end of next month. If the month-level numbers are zero, the daily line will also be flat; ensure the monthly model shows signal first.
* Hourly charts allocate each day’s total using the last **7 days** of observed hourly patterns. Without at least a week of varied timestamps, the allocator falls back to an even 1/24 split per hour and the curve appears flat.
* The MAPE and RMSE badges on the admin page come from cross-validating each model against the **global monthly aggregate**, not individual dishes, so treat them as portfolio-level indicators rather than per-dish guarantees.

### Trend Taco (dish 100)
*History:* Quantities climb linearly from 8 to 112 over twenty-seven months.

| Model | Expected behavior | What to point out |
|-------|-------------------|-------------------|
| Holt–Winters | Projects the rising trend into the mid-110s, showing lower MAPE/RMSE thanks to the longer series. | Trend sensitivity and smooth growth; highlight the tuned smoothing parameters in the “Details” panel. |
| ARIMA(1,0,0) | Lags the slope slightly (~66–68) because it relies on the most recent deviation from the mean. | Its MAPE/RMSE should remain higher than Holt–Winters on this monotonic run but now computes on multiple folds. |
| auto-ARIMA | Picks AR(1) here, landing near the ARIMA value. | Show how auto-selection switches away from the simple mean once the upward drift is evident. |

### Zigzag Soup (dish 101)
*History:* Alternating high/low months (e.g., 38, 12, 36, 14 … with a mid-20s drift into 2026).

| Model | Expected behavior | What to point out |
|-------|-------------------|-------------------|
| Holt–Winters | Dampens oscillations toward the mid-20s rather than chasing each swing; longer history prevents flat fallbacks. | Its RMSE stays modest because smoothing reduces overreaction, and now the metrics use several folds. |
| ARIMA(1,0,0) | Mirrors the zigzag, forecasting back toward the previous high/low (≈30 after a low month, ≈20 after a high month). | Shows how AR terms react to alternating residuals. |
| auto-ARIMA | Chooses AR(1) and produces nearly the same swing-aware path as ARIMA. | Reinforce that automatic selection favors the oscillatory fit. |

### Sparse Salad (dish 102)
*History:* Only three non-zero months spread out (kept intentionally sparse to still show N/A metrics).

| Model | Expected behavior | What to point out |
|-------|-------------------|-------------------|
| All models | Forecasts repeat the last observed value (~10–11) and MAPE/RMSE still show as **N/A** because the history lacks enough folds even with an extra point. | This demonstrates the built-in guardrails for scarce data. |

### Seasonal Shake (dish 103)
*History:* Pronounced summer peaks every year (roughly 6 → 36 → 6), with a mild lift in the second and third years.

| Model | Expected behavior | What to point out |
|-------|-------------------|-------------------|
| Holt–Winters | Tracks the repeating summer spike and off-season trough, projecting another upswing into the next summer. | Good seasonal fit thanks to the 6‑month period; show how smoothing keeps winter months low. |
| ARIMA(1,0,0) | Underestimates the amplitude, trending toward the annual mean rather than the peaks. | Illustrates AR bias toward the average when seasonality dominates. |
| auto-ARIMA | May pick a seasonal/AR configuration that partially captures the wave but remains flatter than Holt. | Contrast automatic selection with manual Holt for strong seasonality. |

### Step Sandwich (dish 104)
*History:* Flat around 14–16 through 2024, then jumps to the mid‑40s in Jan 2025 and trends toward the mid‑60s by Mar 2026.

| Model | Expected behavior | What to point out |
|-------|-------------------|-------------------|
| Holt–Winters | Smooths the level shift and keeps climbing toward the new level. | Demonstrates Holt’s trend component reacting to a structural break. |
| ARIMA(1,0,0) | Lags the step change for several months, easing upward from the old mean. | Showcases AR lag on regime shifts. |
| auto-ARIMA | Typically picks AR(1) here; behaves like ARIMA but may adapt slightly faster depending on automatic order selection. | Use to explain why multiple models are kept for robustness across patterns. |

## Recommendation angle
* Log in as Student A (`a@example.com`) and Student B (`b@example.com`). Student A’s 5★ review on Trend Taco and Student B’s 5★ on Zigzag Soup cause each student to see their favorite dish in the recommendation carousel. Because both students have interacted with different items, the matrix factorization step also surfaces the other’s favorite as a second suggestion, illustrating collaborative filtering.

## Tips for the live session
- Open the “Details” accordion under each dish to show raw monthly history, chosen smoothing parameters, and cross-validation metrics the professors asked about.
- Toggle between models with the dropdown to let them see the different curves on the chart; use Trend Taco vs Zigzag Soup to emphasize trend vs oscillation handling.
- Highlight that Sparse Salad intentionally renders N/A metrics, matching the earlier explanation of insufficient folds even after adding a third point.
- After re-running the SQL, refresh the forecast page to recompute metrics without restarting the app.
