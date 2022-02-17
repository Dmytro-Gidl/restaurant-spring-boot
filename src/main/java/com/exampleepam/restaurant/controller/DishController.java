package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.DishResponseDto;
import com.exampleepam.restaurant.dto.OrderCreationDto;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.DishService;
import com.exampleepam.restaurant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

@Controller
public class DishController {
    private final DishService dishService;
    private final UserService userService;
    private static final String DEFAULT_SORT_FIELD = "name";
    private static final String DEFAULT_SORT_DIR = "asc";
    private static final String DEFAULT_CATEGRY = "all";

    @Autowired
    public DishController(DishService dishService, UserService userService) {
        this.dishService = dishService;
        this.userService = userService;
    }

    @GetMapping("/menu")
    public String returnMenuSorted(
            @RequestParam(value = "sortField",
                    required = false, defaultValue = DEFAULT_SORT_FIELD) String sortField,
            @RequestParam(value = "sortDir",
                    required = false, defaultValue = DEFAULT_SORT_DIR) String sortDir,
            @RequestParam(value = "filterCategory",
                    required = false, defaultValue = DEFAULT_CATEGRY) String filterCategory,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Model model) {


        List<DishResponseDto> dishes;

        if (Objects.equals(filterCategory, DEFAULT_CATEGRY)) {
            dishes = dishService.findAllDishesSorted(sortField, sortDir);
        } else {
            dishes = dishService.findDishesByCategorySorted(sortField, sortDir, filterCategory);
        }

        model.addAttribute("filterCategory", filterCategory);
        model.addAttribute(new OrderCreationDto());
        model.addAttribute("dishList", dishes);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        long userId = authenticatedUser.getUserId();
        model.addAttribute("userBalance", userService.getUserBalance(userId));
        return "menu";
    }

}
