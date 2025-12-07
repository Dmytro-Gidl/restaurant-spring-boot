-- Comprehensive dataset to demonstrate forecasting models and recommendations from a clean database.
-- Run this against an empty test database. All IDs are explicit so reruns overwrite cleanly.

--    Include forecast tables so cached projections don't mix with the new history.
DELETE FROM ingredient_forecast;
DELETE FROM dish_forecast;
DELETE FROM reviews;
DELETE FROM order_item;
DELETE FROM orders;
DELETE FROM dish_ingredient;
DELETE FROM dish;
DELETE FROM ingredient;
DELETE FROM users;

-- 2) Minimal users: one admin to place historical orders, two students to showcase recommendations.
INSERT INTO users (id, name, email, password, balanceuah, role, enabled)
VALUES
  (1, 'Admin Demo', 'admin@example.com', '{noop}pass', 0, 'ADMIN', true),
  (2, 'Student A', 'a@example.com', '{noop}pass', 0, 'USER', true),
  (3, 'Student B', 'b@example.com', '{noop}pass', 0, 'USER', true);

-- 3) Ingredients and dishes with simple compositions.
INSERT INTO ingredient (id, name, unit) VALUES
  (10, 'Tortilla', 'PIECES'),
  (11, 'Chicken', 'GRAMS'),
  (12, 'Mushroom Broth', 'GRAMS'),
  (13, 'Greens', 'GRAMS'),
  (14, 'Berry Puree', 'GRAMS'),
  (15, 'Brioche', 'PIECES');

INSERT INTO dish (id, name, description, category, price, image_file_name, archived)
VALUES
  (100, 'Trend Taco', 'Steady month-over-month growth', 'BURGERS', 12.00, 'taco.jpg', false),
  (101, 'Zigzag Soup', 'Alternating high/low demand', 'SOUPS', 10.00, 'soup.jpg', false),
  (102, 'Sparse Salad', 'Only a few scattered orders', 'SALADS', 8.00, 'salad.jpg', false),
  (103, 'Seasonal Shake', 'Peaks in summer months each year', 'DRINKS', 7.50, 'shake.jpg', false),
  (104, 'Step Sandwich', 'Level shift upward mid-series', 'BURGERS', 11.00, 'sandwich.jpg', false);

INSERT INTO dish_ingredient (id, dish_id, ingredient_id, quantity) VALUES
  (200, 100, 10, 1),
  (201, 100, 11, 150),
  (202, 101, 12, 200),
  (203, 102, 13, 120),
  (204, 103, 14, 180),
  (205, 104, 15, 1);

-- 4) Monthly histories (Jan 2024–Mar 2026) for three contrasting patterns to give models more folds.
-- Trend Taco: linear rise exposes Holt-Winters trend handling.
INSERT INTO orders (id, address, creation_date_time, update_date_time, total_price, status, user_id, reviewed) VALUES
  (1000, 'demo', '2024-01-12 12:00:00', '2024-01-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1001, 'demo', '2024-02-12 12:00:00', '2024-02-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1002, 'demo', '2024-03-12 12:00:00', '2024-03-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1003, 'demo', '2024-04-12 12:00:00', '2024-04-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1004, 'demo', '2024-05-12 12:00:00', '2024-05-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1005, 'demo', '2024-06-12 12:00:00', '2024-06-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1006, 'demo', '2024-07-12 12:00:00', '2024-07-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1007, 'demo', '2024-08-12 12:00:00', '2024-08-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1008, 'demo', '2024-09-12 12:00:00', '2024-09-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1009, 'demo', '2024-10-12 12:00:00', '2024-10-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1010, 'demo', '2024-11-12 12:00:00', '2024-11-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1011, 'demo', '2024-12-12 12:00:00', '2024-12-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1012, 'demo', '2025-01-12 12:00:00', '2025-01-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1013, 'demo', '2025-02-12 12:00:00', '2025-02-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1014, 'demo', '2025-03-12 12:00:00', '2025-03-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1015, 'demo', '2025-04-12 12:00:00', '2025-04-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1016, 'demo', '2025-05-12 12:00:00', '2025-05-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1017, 'demo', '2025-06-12 12:00:00', '2025-06-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1018, 'demo', '2025-07-12 12:00:00', '2025-07-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1019, 'demo', '2025-08-12 12:00:00', '2025-08-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1020, 'demo', '2025-09-12 12:00:00', '2025-09-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1021, 'demo', '2025-10-12 12:00:00', '2025-10-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1022, 'demo', '2025-11-12 12:00:00', '2025-11-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1023, 'demo', '2025-12-12 12:00:00', '2025-12-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1024, 'demo', '2026-01-12 12:00:00', '2026-01-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1025, 'demo', '2026-02-12 12:00:00', '2026-02-12 12:00:00', 0, 'COMPLETED', 1, true),
  (1026, 'demo', '2026-03-12 12:00:00', '2026-03-12 12:00:00', 0, 'COMPLETED', 1, true);
INSERT INTO order_item (id, dish_id, quantity, order_id) VALUES
  (3000, 100, 8, 1000),
  (3001, 100, 12, 1001),
  (3002, 100, 16, 1002),
  (3003, 100, 20, 1003),
  (3004, 100, 24, 1004),
  (3005, 100, 28, 1005),
  (3006, 100, 32, 1006),
  (3007, 100, 36, 1007),
  (3008, 100, 40, 1008),
  (3009, 100, 44, 1009),
  (3010, 100, 48, 1010),
  (3011, 100, 52, 1011),
  (3012, 100, 56, 1012),
  (3013, 100, 60, 1013),
  (3014, 100, 64, 1014),
  (3015, 100, 68, 1015),
  (3016, 100, 72, 1016),
  (3017, 100, 76, 1017),
  (3018, 100, 80, 1018),
  (3019, 100, 84, 1019),
  (3020, 100, 88, 1020),
  (3021, 100, 92, 1021),
  (3022, 100, 96, 1022),
  (3023, 100, 100, 1023),
  (3024, 100, 104, 1024),
  (3025, 100, 108, 1025),
  (3026, 100, 112, 1026);

-- Zigzag Soup: alternating high/low months highlight ARIMA oscillation capture.
INSERT INTO orders (id, address, creation_date_time, update_date_time, total_price, status, user_id, reviewed) VALUES
  (1100, 'demo', '2024-01-05 12:00:00', '2024-01-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1101, 'demo', '2024-02-05 12:00:00', '2024-02-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1102, 'demo', '2024-03-05 12:00:00', '2024-03-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1103, 'demo', '2024-04-05 12:00:00', '2024-04-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1104, 'demo', '2024-05-05 12:00:00', '2024-05-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1105, 'demo', '2024-06-05 12:00:00', '2024-06-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1106, 'demo', '2024-07-05 12:00:00', '2024-07-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1107, 'demo', '2024-08-05 12:00:00', '2024-08-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1108, 'demo', '2024-09-05 12:00:00', '2024-09-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1109, 'demo', '2024-10-05 12:00:00', '2024-10-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1110, 'demo', '2024-11-05 12:00:00', '2024-11-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1111, 'demo', '2024-12-05 12:00:00', '2024-12-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1112, 'demo', '2025-01-05 12:00:00', '2025-01-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1113, 'demo', '2025-02-05 12:00:00', '2025-02-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1114, 'demo', '2025-03-05 12:00:00', '2025-03-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1115, 'demo', '2025-04-05 12:00:00', '2025-04-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1116, 'demo', '2025-05-05 12:00:00', '2025-05-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1117, 'demo', '2025-06-05 12:00:00', '2025-06-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1118, 'demo', '2025-07-05 12:00:00', '2025-07-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1119, 'demo', '2025-08-05 12:00:00', '2025-08-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1120, 'demo', '2025-09-05 12:00:00', '2025-09-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1121, 'demo', '2025-10-05 12:00:00', '2025-10-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1122, 'demo', '2025-11-05 12:00:00', '2025-11-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1123, 'demo', '2025-12-05 12:00:00', '2025-12-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1124, 'demo', '2026-01-05 12:00:00', '2026-01-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1125, 'demo', '2026-02-05 12:00:00', '2026-02-05 12:00:00', 0, 'COMPLETED', 1, true),
  (1126, 'demo', '2026-03-05 12:00:00', '2026-03-05 12:00:00', 0, 'COMPLETED', 1, true);
INSERT INTO order_item (id, dish_id, quantity, order_id) VALUES
  (3100, 101, 38, 1100),
  (3101, 101, 12, 1101),
  (3102, 101, 36, 1102),
  (3103, 101, 14, 1103),
  (3104, 101, 34, 1104),
  (3105, 101, 16, 1105),
  (3106, 101, 32, 1106),
  (3107, 101, 18, 1107),
  (3108, 101, 30, 1108),
  (3109, 101, 20, 1109),
  (3110, 101, 28, 1110),
  (3111, 101, 22, 1111),
  (3112, 101, 26, 1112),
  (3113, 101, 24, 1113),
  (3114, 101, 26, 1114),
  (3115, 101, 32, 1115),
  (3116, 101, 14, 1116),
  (3117, 101, 30, 1117),
  (3118, 101, 16, 1118),
  (3119, 101, 28, 1119),
  (3120, 101, 18, 1120),
  (3121, 101, 26, 1121),
  (3122, 101, 20, 1122),
  (3123, 101, 24, 1123),
  (3124, 101, 22, 1124),
  (3125, 101, 26, 1125),
  (3126, 101, 24, 1126);

-- Sparse Salad: too little data to compute MAPE/RMSE, ideal for showing "N/A" handling.
INSERT INTO orders (id, address, creation_date_time, update_date_time, total_price, status, user_id, reviewed) VALUES
  (1200, 'demo', '2024-04-20 12:00:00', '2024-04-20 12:00:00', 0, 'COMPLETED', 1, true),
  (1201, 'demo', '2024-08-20 12:00:00', '2024-08-20 12:00:00', 0, 'COMPLETED', 1, true),
  (1202, 'demo', '2025-02-20 12:00:00', '2025-02-20 12:00:00', 0, 'COMPLETED', 1, true);
INSERT INTO order_item (id, dish_id, quantity, order_id) VALUES
  (3200, 102, 9, 1200),
  (3201, 102, 11, 1201),
  (3202, 102, 10, 1202);

-- Seasonal Shake: strong summer seasonality across each year with mild off-season volumes.
INSERT INTO orders (id, address, creation_date_time, update_date_time, total_price, status, user_id, reviewed) VALUES
  (1300, 'demo', '2024-01-10 12:00:00', '2024-01-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1301, 'demo', '2024-02-10 12:00:00', '2024-02-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1302, 'demo', '2024-03-10 12:00:00', '2024-03-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1303, 'demo', '2024-04-10 12:00:00', '2024-04-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1304, 'demo', '2024-05-10 12:00:00', '2024-05-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1305, 'demo', '2024-06-10 12:00:00', '2024-06-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1306, 'demo', '2024-07-10 12:00:00', '2024-07-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1307, 'demo', '2024-08-10 12:00:00', '2024-08-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1308, 'demo', '2024-09-10 12:00:00', '2024-09-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1309, 'demo', '2024-10-10 12:00:00', '2024-10-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1310, 'demo', '2024-11-10 12:00:00', '2024-11-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1311, 'demo', '2024-12-10 12:00:00', '2024-12-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1312, 'demo', '2025-01-10 12:00:00', '2025-01-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1313, 'demo', '2025-02-10 12:00:00', '2025-02-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1314, 'demo', '2025-03-10 12:00:00', '2025-03-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1315, 'demo', '2025-04-10 12:00:00', '2025-04-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1316, 'demo', '2025-05-10 12:00:00', '2025-05-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1317, 'demo', '2025-06-10 12:00:00', '2025-06-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1318, 'demo', '2025-07-10 12:00:00', '2025-07-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1319, 'demo', '2025-08-10 12:00:00', '2025-08-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1320, 'demo', '2025-09-10 12:00:00', '2025-09-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1321, 'demo', '2025-10-10 12:00:00', '2025-10-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1322, 'demo', '2025-11-10 12:00:00', '2025-11-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1323, 'demo', '2025-12-10 12:00:00', '2025-12-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1324, 'demo', '2026-01-10 12:00:00', '2026-01-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1325, 'demo', '2026-02-10 12:00:00', '2026-02-10 12:00:00', 0, 'COMPLETED', 1, true),
  (1326, 'demo', '2026-03-10 12:00:00', '2026-03-10 12:00:00', 0, 'COMPLETED', 1, true);
INSERT INTO order_item (id, dish_id, quantity, order_id) VALUES
  (3300, 103, 6, 1300),
  (3301, 103, 6, 1301),
  (3302, 103, 12, 1302),
  (3303, 103, 18, 1303),
  (3304, 103, 28, 1304),
  (3305, 103, 36, 1305),
  (3306, 103, 28, 1306),
  (3307, 103, 18, 1307),
  (3308, 103, 12, 1308),
  (3309, 103, 6, 1309),
  (3310, 103, 6, 1310),
  (3311, 103, 6, 1311),
  (3312, 103, 8, 1312),
  (3313, 103, 8, 1313),
  (3314, 103, 14, 1314),
  (3315, 103, 20, 1315),
  (3316, 103, 30, 1316),
  (3317, 103, 38, 1317),
  (3318, 103, 30, 1318),
  (3319, 103, 20, 1319),
  (3320, 103, 14, 1320),
  (3321, 103, 8, 1321),
  (3322, 103, 8, 1322),
  (3323, 103, 8, 1323),
  (3324, 103, 10, 1324),
  (3325, 103, 10, 1325),
  (3326, 103, 16, 1326);

-- Step Sandwich: structural break around Jan 2025 to illustrate level-shift sensitivity.
INSERT INTO orders (id, address, creation_date_time, update_date_time, total_price, status, user_id, reviewed) VALUES
  (1400, 'demo', '2024-01-18 12:00:00', '2024-01-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1401, 'demo', '2024-02-18 12:00:00', '2024-02-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1402, 'demo', '2024-03-18 12:00:00', '2024-03-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1403, 'demo', '2024-04-18 12:00:00', '2024-04-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1404, 'demo', '2024-05-18 12:00:00', '2024-05-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1405, 'demo', '2024-06-18 12:00:00', '2024-06-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1406, 'demo', '2024-07-18 12:00:00', '2024-07-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1407, 'demo', '2024-08-18 12:00:00', '2024-08-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1408, 'demo', '2024-09-18 12:00:00', '2024-09-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1409, 'demo', '2024-10-18 12:00:00', '2024-10-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1410, 'demo', '2024-11-18 12:00:00', '2024-11-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1411, 'demo', '2024-12-18 12:00:00', '2024-12-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1412, 'demo', '2025-01-18 12:00:00', '2025-01-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1413, 'demo', '2025-02-18 12:00:00', '2025-02-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1414, 'demo', '2025-03-18 12:00:00', '2025-03-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1415, 'demo', '2025-04-18 12:00:00', '2025-04-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1416, 'demo', '2025-05-18 12:00:00', '2025-05-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1417, 'demo', '2025-06-18 12:00:00', '2025-06-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1418, 'demo', '2025-07-18 12:00:00', '2025-07-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1419, 'demo', '2025-08-18 12:00:00', '2025-08-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1420, 'demo', '2025-09-18 12:00:00', '2025-09-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1421, 'demo', '2025-10-18 12:00:00', '2025-10-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1422, 'demo', '2025-11-18 12:00:00', '2025-11-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1423, 'demo', '2025-12-18 12:00:00', '2025-12-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1424, 'demo', '2026-01-18 12:00:00', '2026-01-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1425, 'demo', '2026-02-18 12:00:00', '2026-02-18 12:00:00', 0, 'COMPLETED', 1, true),
  (1426, 'demo', '2026-03-18 12:00:00', '2026-03-18 12:00:00', 0, 'COMPLETED', 1, true);
INSERT INTO order_item (id, dish_id, quantity, order_id) VALUES
  (3400, 104, 15, 1400),
  (3401, 104, 15, 1401),
  (3402, 104, 15, 1402),
  (3403, 104, 15, 1403),
  (3404, 104, 15, 1404),
  (3405, 104, 15, 1405),
  (3406, 104, 16, 1406),
  (3407, 104, 14, 1407),
  (3408, 104, 15, 1408),
  (3409, 104, 15, 1409),
  (3410, 104, 15, 1410),
  (3411, 104, 15, 1411),
  (3412, 104, 45, 1412),
  (3413, 104, 46, 1413),
  (3414, 104, 48, 1414),
  (3415, 104, 50, 1415),
  (3416, 104, 52, 1416),
  (3417, 104, 54, 1417),
  (3418, 104, 56, 1418),
  (3419, 104, 57, 1419),
  (3420, 104, 58, 1420),
  (3421, 104, 59, 1421),
  (3422, 104, 60, 1422),
  (3423, 104, 60, 1423),
  (3424, 104, 62, 1424),
  (3425, 104, 63, 1425),
  (3426, 104, 64, 1426);

-- 5) Reviews to make recommendations visible. Student A loves Trend Taco, Student B loves Zigzag Soup.
INSERT INTO reviews (id, dish_id, order_id, user_id, rating, comment, creation_date_time)
VALUES
  (4000, 100, 1008, 2, 5, 'Perfect for study sessions', '2024-09-13 09:00:00'),
  (4001, 101, 1108, 3, 5, 'Great after lectures', '2024-09-06 09:00:00'),
  (4002, 100, 1007, 3, 3, 'Tasty but spicy', '2024-08-13 09:00:00');
