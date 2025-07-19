package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.dto.review.ReviewDto;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.service.DishService;
import com.exampleepam.restaurant.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


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
    public String viewDishReviews(@PathVariable Long dishId,
                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                  @RequestParam(value = "size", defaultValue = "4") int size,
                                  @RequestParam(value = "sort", defaultValue = "new") String sort,
                                  @RequestParam(value = SORT_FIELD_PARAM, required = false, defaultValue = "name") String sortField,
                                  @RequestParam(value = SORT_DIR_PARAM, required = false, defaultValue = ASCENDING_ORDER_SORTING) String sortDir,
                                  @RequestParam(value = FILTER_CATEGORY_PARAM, required = false, defaultValue = "all") String filterCategory,
                                  Model model) {
        DishResponseDto dish = dishService.getDishById(dishId);
        dish.setAverageRating(reviewService.getAverageRatingForDish(dishId));
        Paged<ReviewDto> reviews = reviewService.getPaginatedReviewsForDish(dishId, page, size, sort);
        model.addAttribute("dish", dish);
        model.addAttribute("reviewPaged", reviews);
        model.addAttribute("pageSize", size);
        model.addAttribute("reviewSort", sort);
        model.addAttribute(SORT_FIELD_PARAM, sortField);
        model.addAttribute(SORT_DIR_PARAM, sortDir);
        model.addAttribute(FILTER_CATEGORY_PARAM, filterCategory);
        return "dish-reviews";
    }
}
