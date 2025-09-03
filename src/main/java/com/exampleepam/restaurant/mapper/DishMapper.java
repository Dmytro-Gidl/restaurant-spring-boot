package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.dish.CategoryDto;
import com.exampleepam.restaurant.dto.dish.DishCreationDto;
import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.DishIngredient;
import com.exampleepam.restaurant.entity.Ingredient;
import com.exampleepam.restaurant.dto.dish.IngredientQuantityDto;
import com.exampleepam.restaurant.repository.IngredientRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper class for Dish and DishDTOs
 */
@Component
public class DishMapper {

  private final IngredientRepository ingredientRepository;

  public DishMapper(IngredientRepository ingredientRepository) {
    this.ingredientRepository = ingredientRepository;
  }

  public DishResponseDto toDishResponseDto(Dish dish) {
    List<IngredientQuantityDto> ingredients = dish.getIngredients().stream()
        .map(di -> new IngredientQuantityDto(
            di.getIngredient().getName(),
            di.getIngredient().getUnit(),
            di.getQuantity()))
        .collect(Collectors.toList());
    return new DishResponseDto(dish.getId(), dish.getName(), dish.getDescription(),
        CategoryDto.valueOf(dish.getCategory().name()), dish.getPrice(),
        dish.getImageFileName(), dish.getGalleryImageFileNames(), ingredients, 0, 0);
  }

  public Dish toDish(DishCreationDto dishCreationDto) {
    Dish dish = new Dish(dishCreationDto.getId(), dishCreationDto.getName(),
        dishCreationDto.getDescription(),
        Category.valueOf(dishCreationDto.getCategory().name()),
        dishCreationDto.getPrice(), dishCreationDto.getImageFileName());
    java.util.List<String> gallery = new java.util.ArrayList<>();
    if (dishCreationDto.getGalleryImageFileNames() != null) {
      for (String n : dishCreationDto.getGalleryImageFileNames()) {
        if (!n.equals(dishCreationDto.getImageFileName())) {
          gallery.add(n);
        }
      }
    }
    dish.setGalleryImageFileNames(gallery);

    if (dishCreationDto.getIngredients() != null) {
      for (IngredientQuantityDto dto : dishCreationDto.getIngredients()) {
        if (dto.getName() == null || dto.getName().isBlank()) continue;
        Ingredient ingredient = ingredientRepository
            .findByNameIgnoreCase(dto.getName())
            .orElseGet(() -> {
              Ingredient newIngredient = new Ingredient();
              newIngredient.setName(dto.getName());
              newIngredient.setUnit(dto.getUnit());
              return newIngredient;
            });
        if (ingredient.getUnit() == null && dto.getUnit() != null) {
          ingredient.setUnit(dto.getUnit());
        }
        DishIngredient di = new DishIngredient();
        di.setDish(dish);
        di.setIngredient(ingredient);
        di.setQuantity(dto.getQuantity());
        dish.getIngredients().add(di);
      }
    }

    return dish;
  }

  public List<DishResponseDto> toDishResponseDtoList(List<Dish> dishes) {
    return dishes.stream()
        .map(this::toDishResponseDto)
        .collect(Collectors.toList());
  }

}
