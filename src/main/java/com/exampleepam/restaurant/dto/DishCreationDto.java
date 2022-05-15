package com.exampleepam.restaurant.dto;

import lombok.*;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Creation DTO for Dish
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DishCreationDto {
    private long id;
    private String imageFileName;
    @NotBlank(message = "{fail.dish.blank.name}")
    @Size(min = 4, max = 30, message = "{fail.dish.size.name}")
    private String name;
    @NotBlank(message = "{fail.dish.blank.description}")
    @Size(min = 4, max = 40, message = "{fail.dish.size.description}")
    private String description;
    @NotNull(message = "{fail.null.category}")
    private CategoryDto category;
    @Digits(integer = 15, fraction = 2, message = "{fail.invalid.price}")
    @Positive(message = "{fail.negative.price}")
    private BigDecimal price;
}

