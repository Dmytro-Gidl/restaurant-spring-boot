package com.exampleepam.restaurant.dto.order;

import com.exampleepam.restaurant.validator.HasOrder;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
  @Size(min = 4, max = 100, message = "{fail.size.address}")
  private String address;
  @HasOrder(message = "{fail.order.absent}")
  private Map<Long, Integer> dishIdQuantityMap;
}

