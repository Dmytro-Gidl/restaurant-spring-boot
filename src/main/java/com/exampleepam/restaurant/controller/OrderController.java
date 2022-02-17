package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.OrderCreationDto;
import com.exampleepam.restaurant.dto.OrderResponseDto;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.exception.InsufficientFundsException;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.DishService;
import com.exampleepam.restaurant.service.OrderService;
import com.exampleepam.restaurant.service.UserService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.util.Map;
import java.util.stream.Collectors;

import static com.exampleepam.restaurant.util.ControllerUtil.filterItemsWithoutOrders;

@Slf4j
@NoArgsConstructor
@Controller
@RequestMapping("/orders")
public class OrderController {
    private  OrderService orderService;
    private  UserService userService;
    private  DishService dishService;

    private static final String DEFAULT_SORT_FIELD = "category";
    private static final String DEFAULT_SORT_DIR = "asc";
    private static final String DEFAULT_CATEGRY = "all";
    private static final String DEFAULT_PAGE_SIZE = "1";

    @Autowired
    public OrderController(OrderService orderService, UserService userService, DishService dishService) {
        this.orderService = orderService;
        this.userService = userService;
        this.dishService = dishService;
    }


    private void fillMenuModelWithData(Model model, AuthenticatedUser authenticatedUser) {
        model.addAttribute("sortField", DEFAULT_SORT_FIELD);
        model.addAttribute("sortDir", DEFAULT_SORT_DIR);
        model.addAttribute("filterCategory", DEFAULT_CATEGRY);
        model.addAttribute("pageSize", DEFAULT_PAGE_SIZE);
        model.addAttribute("filterCategory", DEFAULT_CATEGRY);
        model.addAttribute("dishList", dishService.findAllDishesSorted(
                DEFAULT_SORT_FIELD, DEFAULT_SORT_DIR));
        long userId = authenticatedUser.getUserId();
        model.addAttribute("userBalance", userService.getUserBalance(userId));
    }

    @PostMapping
    public String saveOrder(@Valid @ModelAttribute OrderCreationDto order,
                            BindingResult bindingResult,
                            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                            Model model) {

        if (bindingResult.hasErrors()) {
            fillMenuModelWithData(model, authenticatedUser);
            return "menu";
        }

        Map<Long, Integer> orders = filterItemsWithoutOrders(order);
        order.setDishIdQuantityMap(orders);


        try {
            orderService.saveOrder(order, authenticatedUser);
        } catch (InsufficientFundsException e) {

            log.debug(String.format("User with email %s wanted to order, but balance was too low.",
                    authenticatedUser.getUsername()), e);

            fillMenuModelWithData(model, authenticatedUser);
            bindingResult.reject("insufficient.funds.exception");


            return "menu";
        } catch (EntityNotFoundException e) {
            log.info(String.format("User with email %s wanted to order, but one of items was not found in DB.",
                    authenticatedUser.getUsername()), e);
        }

        return "redirect:/orders/history";
    }


    @GetMapping("/history")
    public String getUserOrderHistoryDefault(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                             Model model) {
        return getUserOrdersPaginated(1,
                "creationDateTime", "desc",
                "all", 10, authenticatedUser, model);
    }


    @GetMapping("/history/page/{pageNo}")
    public String getUserOrdersPaginated(@PathVariable(value = "pageNo", required = false)
                                                 int pageNo,
                                         @RequestParam("sortField") String sortField,
                                         @RequestParam("sortDir") String sortDir,
                                         @RequestParam("status") String statusParam,
                                         @RequestParam("pageSize") int pageSize,
                                         @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                         Model model) {

        Paged<OrderResponseDto> pagedOrder = orderService.findPaginatedByUser(pageNo, pageSize,
                sortField, sortDir, statusParam, authenticatedUser);

        model.addAttribute("status", statusParam);
        model.addAttribute("currentPage", pageNo);

        model.addAttribute("sortField", sortField);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        model.addAttribute("orderList", pagedOrder);

        long userId = authenticatedUser.getUserId();
        model.addAttribute("userBalance", userService.getUserBalance(userId));
        return "order-history";
    }

    @GetMapping
    public String getMappingSupport() {
        return "redirect:/menu";
    }


}
