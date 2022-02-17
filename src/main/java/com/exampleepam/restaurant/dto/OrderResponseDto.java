package com.exampleepam.restaurant.dto;

import com.exampleepam.restaurant.entity.Status;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderResponseDto {
    private long id;
    private Status status;
    private String address;
    private LocalDateTime creationDateTime;
    private LocalDateTime updateDateTime;
    private BigDecimal totalPrice;
    private List<OrderedItemResponseDto> orderItems;


}
