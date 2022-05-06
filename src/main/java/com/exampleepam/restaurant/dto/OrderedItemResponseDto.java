package com.exampleepam.restaurant.dto;

import lombok.*;

/**
 * Response DTO for OrderItem
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrderedItemResponseDto {

    String dishName;
    Integer dishesOrdered;
}

