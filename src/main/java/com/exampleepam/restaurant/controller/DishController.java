package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.dish.DishResponseDto;
import com.exampleepam.restaurant.dto.order.OrderCreationDto;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.DishService;
import com.exampleepam.restaurant.service.recommendation.RecommendationService;
import com.exampleepam.restaurant.entity.paging.Paged;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Dish Controller for Users
 */
@Controller
public class DishController extends BaseController {

  private final DishService dishService;
  private final RecommendationService recommendationService;

  private static final String DEFAULT_SORT_FIELD = "name";
  private static final String DEFAULT_SORT_DIR = ASCENDING_ORDER_SORTING;
  private static final String DEFAULT_CATEGORY = "all";
  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_PAGE_SIZE = 6;

  @Autowired
  public DishController(DishService dishService,
                        RecommendationService recommendationService) {
    this.dishService = dishService;
    this.recommendationService = recommendationService;
  }


  @GetMapping("/menu")
  public String returnMenuSorted(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(value = SORT_FIELD_PARAM,
          required = false, defaultValue = DEFAULT_SORT_FIELD) String sortField,
      @RequestParam(value = SORT_DIR_PARAM,
          required = false, defaultValue = DEFAULT_SORT_DIR) String sortDir,
      @RequestParam(value = FILTER_CATEGORY_PARAM,
          required = false, defaultValue = DEFAULT_CATEGORY) String filterCategory,
      @RequestParam(value = PAGE_NUMBER_PARAM,
          required = false, defaultValue = "" + DEFAULT_PAGE) String pageNoParam,
      @RequestParam(value = PAGE_SIZE_PARAM,
          required = false, defaultValue = "" + DEFAULT_PAGE_SIZE) String pageSizeParam,
      Model model) {

    int pageNo = parseOrDefault(pageNoParam, DEFAULT_PAGE);
    int pageSize = parseOrDefault(pageSizeParam, DEFAULT_PAGE_SIZE);

    String normalizedCategory = filterCategory == null ? DEFAULT_CATEGORY
        : filterCategory.replace("\"", "").toLowerCase();

    String normalizedSortField = sortField == null ? DEFAULT_SORT_FIELD
        : sortField.replace("\"", "").trim();

    String normalizedSortDir = DESCENDING_ORDER_SORTING.equalsIgnoreCase(sortDir)
        ? DESCENDING_ORDER_SORTING
        : ASCENDING_ORDER_SORTING;

    Paged<DishResponseDto> dishPaged = dishService.findPaginated(pageNo, pageSize,
        normalizedSortField, normalizedSortDir, normalizedCategory);

    model.addAttribute(FILTER_CATEGORY_PARAM, normalizedCategory);
    model.addAttribute(CURRENT_PAGE_PARAM, pageNo);
    model.addAttribute(PAGE_SIZE_PARAM, pageSize);
    model.addAttribute(new OrderCreationDto());
    model.addAttribute(DISH_LIST_ATTRIBUTE, dishPaged);
    model.addAttribute(SORT_FIELD_PARAM, normalizedSortField);
    model.addAttribute(SORT_DIR_PARAM, normalizedSortDir);
    model.addAttribute(REVERSE_SORT_DIR_PARAM,
        normalizedSortDir.equals(ASCENDING_ORDER_SORTING) ? DESCENDING_ORDER_SORTING
            : ASCENDING_ORDER_SORTING);

    if (user != null) {
      var recommended = recommendationService.getRecommendedDishes(user.getUserId(), 5);
      model.addAttribute("recommendedDishes", recommended);
    }

    return MENU_PAGE;
  }

  /**
   * Return recommended dishes for the authenticated user as JSON.
   */
  @GetMapping("/api/recommendations")
  @ResponseBody
  public java.util.List<DishResponseDto> getRecommendations(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(value = "limit", defaultValue = "5") int limit) {
    if (user == null) {
      return java.util.List.of();
    }
    return recommendationService.getRecommendedDishes(user.getUserId(), limit);
  }

  private int parseOrDefault(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultValue;
    }
  }
}
