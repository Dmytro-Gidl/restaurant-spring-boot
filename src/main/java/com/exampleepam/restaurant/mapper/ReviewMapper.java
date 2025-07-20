package com.exampleepam.restaurant.mapper;

import com.exampleepam.restaurant.dto.review.ReviewDto;
import com.exampleepam.restaurant.entity.Review;
import org.springframework.stereotype.Component;

/**
 * Simple mapper for converting between {@link Review} entities and
 * {@link ReviewDto} objects. The previous MapStruct based mapper was not
 * generating values correctly which resulted in rating being always 0 and
 * comments being {@code null}. A manual mapper ensures those fields are
 * explicitly copied.
 */
@Component
public class ReviewMapper {

    /**
     * Convert a DTO coming from the form into a new {@link Review} entity. The
     * dish and user relations are set separately in the service layer.
     */
    public Review toEntity(ReviewDto reviewDto) {
        if (reviewDto == null) {
            return null;
        }
        Review review = new Review();
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        return review;
    }

    /**
     * Convert a persisted {@link Review} entity into a DTO for rendering.
     */
    public ReviewDto toDto(Review review) {
        if (review == null) {
            return null;
        }
        ReviewDto dto = new ReviewDto();
        // Use the review id for DTO id as it is not used elsewhere
        dto.setId(review.getId() == null ? 0 : review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreationDateTime(review.getCreationDateTime());
        if (review.getUser() != null) {
            dto.setUserName(review.getUser().getName());
        }
        return dto;
    }
}
