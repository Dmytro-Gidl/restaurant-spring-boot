package com.exampleepam.restaurant.dto;

import com.exampleepam.restaurant.entity.Status;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
