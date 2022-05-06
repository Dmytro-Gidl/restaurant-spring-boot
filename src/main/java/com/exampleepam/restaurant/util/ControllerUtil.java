package com.exampleepam.restaurant.util;

import com.exampleepam.restaurant.dto.OrderCreationDto;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Util class for Controller
 */
public final class ControllerUtil {
    private ControllerUtil() {
    }

    public static Map<Long, Integer> filterItemsWithoutOrders(OrderCreationDto orderCreationDto) {
        return orderCreationDto.getDishIdQuantityMap()
                .entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }
}
