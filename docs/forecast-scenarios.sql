-- Sample data sets for experimenting with forecasting models.
-- Run one scenario at a time in a test database.
-- Assumes user with id 1 and dish with id 1 already exist.

-- Scenario 1: Steady growth (orders increase by 5 each month)
DELETE FROM order_item;
DELETE FROM orders;
INSERT INTO orders (id, address, creation_date_time, update_date_time, total_price, status, user_id, reviewed) VALUES
  (1001, 'test', '2024-01-15 12:00:00', '2024-01-15 12:00:00', 0, 'COMPLETED', 1, false),
  (1002, 'test', '2024-02-15 12:00:00', '2024-02-15 12:00:00', 0, 'COMPLETED', 1, false),
  (1003, 'test', '2024-03-15 12:00:00', '2024-03-15 12:00:00', 0, 'COMPLETED', 1, false),
  (1004, 'test', '2024-04-15 12:00:00', '2024-04-15 12:00:00', 0, 'COMPLETED', 1, false),
  (1005, 'test', '2024-05-15 12:00:00', '2024-05-15 12:00:00', 0, 'COMPLETED', 1, false),
  (1006, 'test', '2024-06-15 12:00:00', '2024-06-15 12:00:00', 0, 'COMPLETED', 1, false);
INSERT INTO order_item (id, dish_id, quantity, order_id) VALUES
  (2001, 1, 5, 1001),
  (2002, 1,10, 1002),
  (2003, 1,15, 1003),
  (2004, 1,20, 1004),
  (2005, 1,25, 1005),
  (2006, 1,30, 1006);

-- Scenario 2: Mean-reverting demand (high then low months)
DELETE FROM order_item;
DELETE FROM orders;
INSERT INTO orders (id, address, creation_date_time, update_date_time, total_price, status, user_id, reviewed) VALUES
  (2001, 'test', '2024-01-15 12:00:00', '2024-01-15 12:00:00', 0, 'COMPLETED', 1, false),
  (2002, 'test', '2024-02-15 12:00:00', '2024-02-15 12:00:00', 0, 'COMPLETED', 1, false),
  (2003, 'test', '2024-03-15 12:00:00', '2024-03-15 12:00:00', 0, 'COMPLETED', 1, false),
  (2004, 'test', '2024-04-15 12:00:00', '2024-04-15 12:00:00', 0, 'COMPLETED', 1, false),
  (2005, 'test', '2024-05-15 12:00:00', '2024-05-15 12:00:00', 0, 'COMPLETED', 1, false),
  (2006, 'test', '2024-06-15 12:00:00', '2024-06-15 12:00:00', 0, 'COMPLETED', 1, false);
INSERT INTO order_item (id, dish_id, quantity, order_id) VALUES
  (3001, 1,20,2001),
  (3002, 1,5, 2002),
  (3003, 1,20,2003),
  (3004, 1,5, 2004),
  (3005, 1,20,2005),
  (3006, 1,5, 2006);

-- Scenario 3: Sparse sporadic orders (only a few months)
DELETE FROM order_item;
DELETE FROM orders;
INSERT INTO orders (id, address, creation_date_time, update_date_time, total_price, status, user_id, reviewed) VALUES
  (3001, 'test', '2024-03-15 12:00:00', '2024-03-15 12:00:00', 0, 'COMPLETED', 1, false),
  (3002, 'test', '2024-07-15 12:00:00', '2024-07-15 12:00:00', 0, 'COMPLETED', 1, false),
  (3003, 'test', '2024-12-15 12:00:00', '2024-12-15 12:00:00', 0, 'COMPLETED', 1, false);
INSERT INTO order_item (id, dish_id, quantity, order_id) VALUES
  (4001, 1,10,3001),
  (4002, 1,12,3002),
  (4003, 1,11,3003);
