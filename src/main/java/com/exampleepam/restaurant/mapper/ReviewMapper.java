package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.review.ReviewDto;
import com.exampleepam.restaurant.entity.Review;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    Review toEntity(ReviewDto reviewDto);

    ReviewDto toDto(Review review);
}
