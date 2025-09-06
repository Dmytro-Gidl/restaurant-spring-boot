package com.exampleepam.restaurant.forecast;

import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.repository.OrderRepository;
import com.exampleepam.restaurant.service.forecast.HistoryCollector;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HistoryCollectorTest {

    @Test
    void aggregatesHourlyDailyAndMonthlyTotals() {
        OrderRepository repo = Mockito.mock(OrderRepository.class);
        HistoryCollector collector = new HistoryCollector(repo);

        Dish dish = new Dish();
        dish.setId(1L);

        Order o1 = new Order();
        o1.setStatus(Status.COMPLETED);
        o1.setCreationDateTime(LocalDateTime.of(2023,1,1,10,0));
        o1.setOrderItems(new ArrayList<>());
        o1.addOrderItem(new OrderItem(dish,2));

        Order o2 = new Order();
        o2.setStatus(Status.COMPLETED);
        o2.setCreationDateTime(LocalDateTime.of(2023,1,1,12,0));
        o2.setOrderItems(new ArrayList<>());
        o2.addOrderItem(new OrderItem(dish,3));

        Order o3 = new Order();
        o3.setStatus(Status.COMPLETED);
        o3.setCreationDateTime(LocalDateTime.of(2023,2,2,9,0));
        o3.setOrderItems(new ArrayList<>());
        o3.addOrderItem(new OrderItem(dish,5));

        Mockito.when(repo.findByStatusAndCreationDateTimeAfter(Mockito.eq(Status.COMPLETED), Mockito.any()))
                .thenReturn(List.of(o1,o2,o3));

        HistoryCollector.History history = collector.collect(LocalDateTime.of(2022,12,1,0,0));

        int[] janHours = history.hourlyTotals.get(1L).get(LocalDate.of(2023,1,1));
        assertEquals(2, janHours[10]);
        assertEquals(3, janHours[12]);

        assertEquals(5, history.dailyTotals.get(1L).get(LocalDate.of(2023,1,1)));
        assertEquals(5, history.monthlyTotals.get(1L).get(YearMonth.of(2023,1)));
        assertEquals(5, history.monthlyTotals.get(1L).get(YearMonth.of(2023,2)));
        assertEquals(5, history.globalMonthly.get(YearMonth.of(2023,1)));
        assertEquals(5, history.globalMonthly.get(YearMonth.of(2023,2)));
    }
}
