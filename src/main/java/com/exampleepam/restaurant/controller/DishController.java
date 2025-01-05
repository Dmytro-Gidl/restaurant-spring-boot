package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.DishResponseDto;
import com.exampleepam.restaurant.dto.OrderCreationDto;
import com.exampleepam.restaurant.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

/**
 * Dish Controller for Users
 */
@Controller
public class DishController extends BaseController {

    private final DishService dishService;

    private static final String DEFAULT_SORT_FIELD = "name";
    private static final String DEFAULT_SORT_DIR = ASCENDING_ORDER_SORTING;
    private static final String DEFAULT_CATEGORY = "all";

    @Autowired
    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping("/menu")
    public String returnMenuSorted(
            @RequestParam(value = SORT_FIELD_PARAM,
                    required = false, defaultValue = DEFAULT_SORT_FIELD) String sortField,
            @RequestParam(value = SORT_DIR_PARAM,
                    required = false, defaultValue = DEFAULT_SORT_DIR) String sortDir,
            @RequestParam(value = FILTER_CATEGORY_PARAM,
                    required = false, defaultValue = DEFAULT_CATEGORY) String filterCategory,
            Model model) {

        List<DishResponseDto> dishes;

        if (Objects.equals(filterCategory, DEFAULT_CATEGORY)) {
            dishes = dishService.findAllDishesSorted(sortField, sortDir);
        } else {
            dishes = dishService.findDishesByCategorySorted(sortField, sortDir, filterCategory);
        }

        model.addAttribute(FILTER_CATEGORY_PARAM, filterCategory);
        model.addAttribute(new OrderCreationDto());
        model.addAttribute(DISH_LIST_ATTRIBUTE, dishes);
        model.addAttribute(SORT_FIELD_PARAM, sortField);
        model.addAttribute(SORT_DIR_PARAM, sortDir);
        model.addAttribute(REVERSE_SORT_DIR_PARAM,
                sortDir.equals(ASCENDING_ORDER_SORTING) ? DESCENDING_ORDER_SORTING : ASCENDING_ORDER_SORTING);
        return MENU_PAGE;
    }
}
