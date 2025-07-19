package com.exampleepam.restaurant.dto.dish;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response DTO for Dish
 */
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
  private double averageRating;

  public String getimagePath() {
    if (imageFileName == null || id == 0) {
      return null;
    }

    return "/dish-images/" + id + "/" + imageFileName;
  }

}
