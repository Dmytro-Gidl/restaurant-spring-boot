package com.exampleepam.restaurant.service.forecast;

import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Autowired
    public HistoryCollector(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public History collect(LocalDateTime start) {
        List<Order> orders = orderRepository.findByStatusAndCreationDateTimeAfter(Status.COMPLETED, start);
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
    }
}
