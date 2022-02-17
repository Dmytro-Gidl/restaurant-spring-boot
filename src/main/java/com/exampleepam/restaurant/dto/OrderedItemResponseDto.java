package com.exampleepam.restaurant.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrderedItemResponseDto {

    String dishName;
    Integer dishesOrdered;
}

