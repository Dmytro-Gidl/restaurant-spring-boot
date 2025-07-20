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
  private java.util.List<String> galleryImageFileNames;
  private double averageRating;
  private long reviewCount;

  public String getimagePath() {
    if (imageFileName == null || id == 0) {
      return null;
    }

    return "/dish-images/" + id + "/" + imageFileName;
  }

  public java.util.List<String> getAllImageFileNames() {
    java.util.List<String> result = new java.util.ArrayList<>();
    if (imageFileName != null) {
      result.add(imageFileName);
    }
    if (galleryImageFileNames != null) {
      for (String n : galleryImageFileNames) {
        if (!n.equals(imageFileName)) {
          result.add(n);
        }
      }
    }
    return result;
  }

  public java.util.List<String> getImagePaths() {
    java.util.List<String> list = new java.util.ArrayList<>();
    for (String name : getAllImageFileNames()) {
      list.add("/dish-images/" + id + "/" + name);
    }
    return list;
  }

  public String getImagePathsString() {
    return String.join(",", getImagePaths());
  }

}
