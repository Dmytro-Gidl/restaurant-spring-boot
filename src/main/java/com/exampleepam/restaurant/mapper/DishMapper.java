package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.CategoryDto;
import com.exampleepam.restaurant.dto.DishCreationDto;
import com.exampleepam.restaurant.dto.DishResponseDto;
import com.exampleepam.restaurant.entity.Category;
import com.exampleepam.restaurant.entity.Dish;
import org.springframework.stereotype.Component;

@Component
public class DishMapper {

    public DishResponseDto toDishResponseDto(Dish dish) {
        return new DishResponseDto(dish.getId(), dish.getName(), dish.getDescription(),
                CategoryDto.valueOf(dish.getCategory().name()), dish.getPrice(), dish.getImageFileName());
    }
    public Dish toDish(DishCreationDto dishCreationDto) {
        return new Dish(dishCreationDto.getId(), dishCreationDto.getName(),
                dishCreationDto.getDescription(),
                Category.valueOf(dishCreationDto.getCategory().name()),
                dishCreationDto.getPrice(), dishCreationDto.getImageFileName());
    }

}
