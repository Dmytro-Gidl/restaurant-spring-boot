package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for admin operations on reviews.
 */
@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    @Autowired
    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @DeleteMapping("/{id}")
    public String deleteReview(@PathVariable("id") Long reviewId,
                               @RequestParam("dishId") Long dishId) {
        reviewService.deleteReview(reviewId);
        return "redirect:/dishes/" + dishId + "/reviews";
    }
}
