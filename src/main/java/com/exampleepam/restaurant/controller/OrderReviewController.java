package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.ReviewForm;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/orders")
public class OrderReviewController {

    private final ReviewService reviewService;

    @Autowired
    public OrderReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Returns a page to review all dishes in a single order.
     * GET /orders/{orderId}/reviews
     */
    @GetMapping("/{orderId}/reviews")
    public String getReviewPage(@PathVariable("orderId") Long orderId, Model model) {
        // If you need to validate that this order belongs to the current user, do it here

        model.addAttribute("orderId", orderId);
        // A list of DishResponseDto or something similar
        model.addAttribute("dishes", reviewService.getDishesForOrder(orderId));
        model.addAttribute("reviewForm", new ReviewForm());
        return "review";
    }

    /**
     * Submits reviews for multiple dishes in the specified order.
     * POST /orders/{orderId}/reviews
     */
    @PostMapping("/{orderId}/reviews")
    public String submitOrderReviews(@PathVariable Long orderId,
                                     @ModelAttribute("reviewForm") ReviewForm form,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        reviewService.submitReviews(orderId, user.getUserId(), form.getReviews());
        log.info("order Id: {}, ", orderId);
        log.info("reviewForm: {}, ", form);
        return "redirect:/orders/history";
    }
}
