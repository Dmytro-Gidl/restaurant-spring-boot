package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.ReviewDto;
import com.exampleepam.restaurant.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(source = "rating", target = "rating")
    @Mapping(source = "comment", target = "comment")
    Review toEntity(ReviewDto reviewDto);

    ReviewDto toDto(Review review);
}
