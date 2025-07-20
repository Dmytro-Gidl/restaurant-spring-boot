package com.exampleepam.restaurant.dto.order;

import com.exampleepam.restaurant.entity.Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response DTO for Order
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class OrderResponseDto {

  private long id;
  private Status status;
  private String address;
  private boolean isReviewed;
  private LocalDateTime creationDateTime;
  private LocalDateTime updateDateTime;
  private BigDecimal totalPrice;
  private String clientName;
  private List<OrderedItemResponseDto> orderItems;
}
