package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.OrderCreationDto;
import com.exampleepam.restaurant.dto.OrderResponseDto;
import com.exampleepam.restaurant.dto.OrderedItemResponseDto;
import com.exampleepam.restaurant.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper class for Order and OrderDTOs
 */
@Component
public class OrderMapper {

    public Order toOrder(OrderCreationDto orderCreationDto, User user, Map<Dish, Integer> dishQuantityMap) {

        BigDecimal totalPrice = getOrderTotalPrice(dishQuantityMap);
        List<OrderItem> orderItems = getOrderItems(dishQuantityMap);
        Order order = new Order(0, Status.PENDING, totalPrice, orderCreationDto.getAddress(), user);
        orderItems.forEach(order::addOrderItem);
        return order;
    }


    private BigDecimal getOrderTotalPrice(Map<Dish, Integer> dishQuantityMap) {
        return dishQuantityMap.entrySet().stream()
                .map(d -> d.getKey().getPrice().multiply(new BigDecimal(d.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OrderItem> getOrderItems(Map<Dish, Integer> dishQuantityMap) {
        return dishQuantityMap.entrySet().stream()
                .map(entry -> new OrderItem(entry.getKey().getName(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public OrderResponseDto toOrderResponseDto(Order order) {
        List<OrderedItemResponseDto> orderedItemResponseDtos = order.getOrderItems().stream()
                .map(orderItem ->
                        new OrderedItemResponseDto(orderItem.getDishName(), orderItem.getQuantity()))
                .collect(Collectors.toList());
        return new OrderResponseDto(order.getId(), order.getStatus(), order.getAddress(),
                order.getCreationDateTime(), order.getUpdateDateTime(),
                order.getTotalPrice(), orderedItemResponseDtos);
    }
}

