package com.exampleepam.restaurant.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DishResponseDto {
    private long id;
    private String name;
    private String description;
    private CategoryDto category;
    private BigDecimal price;
    private String imageFileName;

    public String getimagePath() {
        if (imageFileName == null || id == 0) return null;

        return "/dish-images/" + id + "/" + imageFileName;
    }

}
