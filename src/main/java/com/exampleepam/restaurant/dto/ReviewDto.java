package com.exampleepam.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    private long id; // Referencing the dish by ID, not the full entity

    @NotNull
    @Min(1) // Minimum rating value
    @Max(5) // Maximum rating value
    private int rating;

    @NotBlank // Comment cannot be blank
    @Size(max = 300, message = "Comment can be at most 300 characters")
    private String comment;
}
