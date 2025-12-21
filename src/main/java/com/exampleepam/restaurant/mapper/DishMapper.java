package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.dish.CategoryDto;
import com.exampleepam.restaurant.dto.dish.DishCreationDto;
import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.dto.dish.IngredientQuantityDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import com.exampleepam.restaurant.entity.DishIngredient;
import com.exampleepam.restaurant.entity.Ingredient;
import com.exampleepam.restaurant.repository.IngredientRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                .map(this::toIngredientQuantityDto)
                .collect(Collectors.toList());

        return new DishResponseDto(
                dish.getId(),
                dish.getName(),
                dish.getDescription(),
                CategoryDto.valueOf(dish.getCategory().name()),
                dish.getPrice(),
                dish.getImageFileName(),
                dish.getGalleryImageFileNames(),
                ingredients,
                0,
                0
        );
    }

    public Dish toDish(DishCreationDto dto) {
        Dish dish = new Dish(
                dto.getId(),
                dto.getName(),
                dto.getDescription(),
                Category.valueOf(dto.getCategory().name()),
                dto.getPrice(),
                dto.getImageFileName()
        );
        dish.setGalleryImageFileNames(filteredGallery(dto));

        if (dto.getIngredients() != null) {
            dto.getIngredients().stream()
                    .filter(this::hasName)
                    .map(ingredientDto -> toDishIngredient(ingredientDto, dish))
                    .forEach(di -> dish.getIngredients().add(di));
        }
        return dish;
    }

    public List<DishResponseDto> toDishResponseDtoList(List<Dish> dishes) {
        return dishes.stream()
                .map(this::toDishResponseDto)
                .collect(Collectors.toList());
    }

    // ---------- private helpers ----------

    private IngredientQuantityDto toIngredientQuantityDto(DishIngredient di) {
        return new IngredientQuantityDto(
                di.getIngredient().getName(),
                di.getIngredient().getUnit(),
                di.getQuantity()
        );
    }

    private boolean hasName(IngredientQuantityDto dto) {
        return dto.getName() != null && !dto.getName().isBlank();
    }

    private List<String> filteredGallery(DishCreationDto dto) {
        List<String> gallery = new ArrayList<>();
        if (dto.getGalleryImageFileNames() == null) {
            return gallery;
        }

        String mainImage = dto.getImageFileName();
        for (String name : dto.getGalleryImageFileNames()) {
            if (!name.equals(mainImage)) {
                gallery.add(name);
            }
        }

        return gallery;
    }

    private DishIngredient toDishIngredient(IngredientQuantityDto ingredientDto, Dish dish) {
        Ingredient ingredient = ingredientRepository
                .findByNameIgnoreCase(ingredientDto.getName())
                .orElseGet(() -> createNewIngredient(ingredientDto));

        // set unit if it's not already known
        if (ingredient.getUnit() == null && ingredientDto.getUnit() != null) {
            ingredient.setUnit(ingredientDto.getUnit());
        }

        DishIngredient dishIngredient = new DishIngredient();
        dishIngredient.setDish(dish);
        dishIngredient.setIngredient(ingredient);
        dishIngredient.setQuantity(ingredientDto.getQuantity());

        return dishIngredient;
    }

    private Ingredient createNewIngredient(IngredientQuantityDto dto) {
        Ingredient newIngredient = new Ingredient();
        newIngredient.setName(dto.getName());
        newIngredient.setUnit(dto.getUnit());
        return newIngredient;
    }
}
