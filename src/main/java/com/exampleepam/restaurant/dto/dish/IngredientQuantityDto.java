package com.exampleepam.restaurant.dto.dish;

import com.exampleepam.restaurant.entity.MeasureUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO representing ingredient usage in a dish with required quantity and unit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IngredientQuantityDto {
    private String name;
    private MeasureUnit unit;
    private int quantity;
}
