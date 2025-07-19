package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.dto.review.ReviewDto;
import com.exampleepam.restaurant.service.DishService;
import com.exampleepam.restaurant.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class DishReviewController extends BaseController {

    private final ReviewService reviewService;
    private final DishService dishService;

    @Autowired
    public DishReviewController(ReviewService reviewService, DishService dishService) {
        this.reviewService = reviewService;
        this.dishService = dishService;
    }

    @GetMapping("/dishes/{dishId}/reviews")
    public String viewDishReviews(@PathVariable Long dishId, Model model) {
        DishResponseDto dish = dishService.getDishById(dishId);
        dish.setAverageRating(reviewService.getAverageRatingForDish(dishId));
        List<ReviewDto> reviews = reviewService.getReviewsForDish(dishId);
        model.addAttribute("dish", dish);
        model.addAttribute("reviews", reviews);
        return "dish-reviews";
    }
}
