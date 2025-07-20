package com.exampleepam.restaurant.dto.order;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response DTO for OrderItem
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderedItemResponseDto {

  String dishName;
  Integer dishesOrdered;
  private BigDecimal dishPrice;
  private String imagePath;
}
