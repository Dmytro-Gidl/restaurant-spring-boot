package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.order.OrderCreationDto;
import com.exampleepam.restaurant.dto.order.OrderResponseDto;
import com.exampleepam.restaurant.dto.order.OrderedItemResponseDto;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.Order;
import com.exampleepam.restaurant.entity.OrderItem;
import com.exampleepam.restaurant.entity.Status;
import com.exampleepam.restaurant.entity.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper class for Order and OrderDTOs
 */
@Component
public class OrderMapper {

  public Order toOrder(OrderCreationDto orderCreationDto, User user,
      Map<Dish, Integer> dishQuantityMap) {

    BigDecimal totalPrice = getOrderTotalPrice(dishQuantityMap);
    List<OrderItem> orderItems = getOrderItems(dishQuantityMap);
    Order order = new Order(0, Status.PENDING, totalPrice, orderCreationDto.getAddress(), user);
    orderItems.forEach(order::addOrderItem);
    return order;
  }


  private BigDecimal getOrderTotalPrice(Map<Dish, Integer> dishQuantityMap) {
    return dishQuantityMap.entrySet().stream()
        .map(e -> e.getKey().getPrice().multiply(BigDecimal.valueOf(e.getValue())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private List<OrderItem> getOrderItems(Map<Dish, Integer> dishQuantityMap) {
    return dishQuantityMap.entrySet().stream()
        .map(entry -> new OrderItem(entry.getKey(), entry.getValue()))
        .toList();
  }

  public OrderResponseDto toOrderResponseDto(Order order) {
    List<OrderedItemResponseDto> orderedItemResponseDtos = order.getOrderItems().stream()
        .map(this::mapToOrderedItemResponseDto)
        .collect(Collectors.toList());

    return OrderResponseDto.builder()
        .id(order.getId())
        .status(order.getStatus())
        .address(order.getAddress())
        .creationDateTime(order.getCreationDateTime())
        .updateDateTime(order.getUpdateDateTime())
        .totalPrice(order.getTotalPrice())
        .clientName(order.getUser().getName())
        .orderItems(orderedItemResponseDtos)
        .isReviewed(order.getReview() != null)
        .build();
  }

  private OrderedItemResponseDto mapToOrderedItemResponseDto(OrderItem orderItem) {
    return OrderedItemResponseDto.builder()
        .dishName(orderItem.getDish().getName())
        .dishesOrdered(orderItem.getQuantity())
        .dishPrice(orderItem.getDish().getPrice())
        .imagePath(orderItem.getDish().getimagePath())
        .build();
  }
}
