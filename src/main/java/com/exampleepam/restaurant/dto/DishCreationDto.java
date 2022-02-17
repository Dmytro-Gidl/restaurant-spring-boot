package com.exampleepam.restaurant.dto;

import lombok.*;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DishCreationDto {
    private long id;
    private String imageFileName;
    @NotBlank(message = "{fail.blank.name}")
    private String name;
    @NotBlank(message = "{fail.blank.description}")
    private String description;
    @NotNull(message = "{fail.null.category}")
    private CategoryDto category;
    @Digits(integer = 15, fraction = 2, message = "{fail.invalid.price}")
    @Positive(message = "{fail.negative.price}")
    private BigDecimal price;
}

