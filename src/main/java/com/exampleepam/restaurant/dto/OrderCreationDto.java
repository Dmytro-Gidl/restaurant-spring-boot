package com.exampleepam.restaurant.dto;

import com.exampleepam.restaurant.validator.HasOrder;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OrderCreationDto {
    @NotBlank(message = "{fail.blank.address}")
    private String address;
    @HasOrder(message = "{fail.order.absent}")
    private Map<Long, Integer> dishIdQuantityMap;
}

