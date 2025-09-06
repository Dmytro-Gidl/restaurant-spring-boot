package com.exampleepam.restaurant.service.forecast;

import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Component
public class HistoryCollector {

    private final OrderRepository orderRepository;
    private static final Logger log = LoggerFactory.getLogger(HistoryCollector.class);

    @Autowired
    public HistoryCollector(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public History collect(LocalDateTime start) {
        List<Order> orders = orderRepository.findByStatusAndCreationDateTimeAfter(Status.COMPLETED, start);
        log.debug("Fetched {} completed orders since {}", orders.size(), start);
        if (orders.isEmpty()) {
            log.warn("No completed orders found since {}", start);
        }
        History history = new History();
        for (Order order : orders) {
            LocalDateTime dateTime = order.getCreationDateTime();
            LocalDate date = dateTime.toLocalDate();
            int hour = dateTime.getHour();
            YearMonth ym = YearMonth.from(dateTime);
            for (OrderItem item : order.getOrderItems()) {
                long dishId = item.getDish().getId();
                int qty = item.getQuantity();
                history.hourlyTotals.computeIfAbsent(dishId, k -> new HashMap<>())
                        .computeIfAbsent(date, d -> new int[24])[hour] += qty;
                history.dailyTotals.computeIfAbsent(dishId, k -> new HashMap<>())
                        .merge(date, qty, Integer::sum);
                history.monthlyTotals.computeIfAbsent(dishId, k -> new HashMap<>())
                        .merge(ym, qty, Integer::sum);
                history.globalMonthly.merge(ym, qty, Integer::sum);
            }
        }
        history.monthlyTotals.forEach((id, map) -> {
            log.debug("Dish {} monthly totals {}", id, map);
            if (map.isEmpty()) {
                log.warn("Dish {} has no completed data", id);
            }
        });
        boolean allZero = history.globalMonthly.values().stream().allMatch(v -> v == 0);
        if (allZero) {
            log.warn("Collected history contains only zero monthly totals");
        }
        return history;
    }

    public static class History {
        public final Map<Long, Map<LocalDate, int[]>> hourlyTotals = new HashMap<>();
        public final Map<Long, Map<LocalDate, Integer>> dailyTotals = new HashMap<>();
        public final Map<Long, Map<YearMonth, Integer>> monthlyTotals = new HashMap<>();
        public final Map<YearMonth, Integer> globalMonthly = new HashMap<>();

        public List<Integer> globalMonthlyTotals() {
            YearMonth current = YearMonth.now();
            YearMonth start = current.minusMonths(24);
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i <= 24; i++) {
                YearMonth ym = start.plusMonths(i);
                list.add(globalMonthly.getOrDefault(ym, 0));
            }
            return list;
        }

        public boolean hasMonthlyData(long dishId) {
            return monthlyTotals.containsKey(dishId) && !monthlyTotals.get(dishId).isEmpty();
        }

        /**
         * Export the aggregated monthly history to a CSV file for external
         * benchmarking or analysis.
         */
        public void exportMonthlyCsv(java.nio.file.Path path) throws java.io.IOException {
            YearMonth current = YearMonth.now();
            YearMonth start = current.minusMonths(24);
            try (java.io.BufferedWriter w = java.nio.file.Files.newBufferedWriter(path)) {
                w.write("month,quantity\n");
                for (int i = 0; i <= 24; i++) {
                    YearMonth ym = start.plusMonths(i);
                    int qty = globalMonthly.getOrDefault(ym, 0);
                    w.write(ym + "," + qty + "\n");
                }
            }
        }
    }
}
