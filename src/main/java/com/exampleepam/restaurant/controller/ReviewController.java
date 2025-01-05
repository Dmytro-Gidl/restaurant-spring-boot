package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.ReviewDto;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/dish/{dishId}")
    public String submitReview(
            @PathVariable("dishId") Long dishId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam("rating") int rating,
            @RequestParam("comment") String comment
    ) {
        ReviewDto reviewDto = new ReviewDto(dishId, rating, comment);
        reviewService.saveReview(reviewDto, authenticatedUser);
        return "redirect:/menu";
    }
}
