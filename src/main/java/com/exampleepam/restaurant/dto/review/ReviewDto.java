package com.exampleepam.restaurant.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReviewDto {

    @NotNull
    private long id;

    @NotNull
    @Min(1)
    @Max(5)
    private int rating;

    @NotBlank
    @Size(max = 300, message = "Comment can be at most 300 characters")
    private String comment;

    /**
     * Name of the user who left the review. Not required when submitting a review,
     * but used for displaying reviews on the site.
     */
    private String userName;

    /**
     * Timestamp when the review was created.
     */
    private LocalDateTime creationDateTime;
}
