package com.exampleepam.restaurant.dto;

import com.exampleepam.restaurant.validator.HasOrder;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * Creation DTO for Order
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OrderCreationDto {
    @NotBlank(message = "{fail.blank.address}")
    @Size(min = 4, max = 35, message = "{fail.size.address}")
    private String address;
    @HasOrder(message = "{fail.order.absent}")
    private Map<Long, Integer> dishIdQuantityMap;
}

