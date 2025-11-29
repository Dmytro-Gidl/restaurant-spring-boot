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
  (13, 'Greens', 'GRAMS');

INSERT INTO dish (id, name, description, category, price, image_file_name, archived)
VALUES
  (100, 'Trend Taco', 'Steady month-over-month growth', 'BURGERS', 12.00, 'taco.jpg', false),
  (101, 'Zigzag Soup', 'Alternating high/low demand', 'SOUPS', 10.00, 'soup.jpg', false),
  (102, 'Sparse Salad', 'Only a few scattered orders', 'SALADS', 8.00, 'salad.jpg', false);

INSERT INTO dish_ingredient (id, dish_id, ingredient_id, quantity) VALUES
  (200, 100, 10, 1),
  (201, 100, 11, 150),
  (202, 101, 12, 200),
  (203, 102, 13, 120);

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

-- 5) Reviews to make recommendations visible. Student A loves Trend Taco, Student B loves Zigzag Soup.
INSERT INTO reviews (id, dish_id, order_id, user_id, rating, comment, creation_date_time)
VALUES
  (4000, 100, 1008, 2, 5, 'Perfect for study sessions', '2024-09-13 09:00:00'),
  (4001, 101, 1108, 3, 5, 'Great after lectures', '2024-09-06 09:00:00'),
  (4002, 100, 1007, 3, 3, 'Tasty but spicy', '2024-08-13 09:00:00');
