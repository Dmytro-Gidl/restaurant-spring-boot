package com.exampleepam.restaurant.controller;

import static com.exampleepam.restaurant.util.ControllerUtil.filterItemsWithoutOrders;

import com.exampleepam.restaurant.dto.order.OrderCreationDto;
import com.exampleepam.restaurant.dto.order.OrderResponseDto;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.exception.InsufficientFundsException;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.DishService;
import com.exampleepam.restaurant.service.OrderService;
import com.exampleepam.restaurant.service.UserService;
import java.util.Map;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Order Controller for Users
 */
@Slf4j
@NoArgsConstructor
@Controller
@RequestMapping("/orders")
public class OrderController extends BaseController {

  private static final String REDIRECT_TO_ORDERS_HISTORY = "redirect:/orders/history";
  private static final String REDIRECT_TO_ORDERS_HISTORY_CLEAR =
      "redirect:/orders/history?clearCart";
  private static final String ORDER_HISTORY_PAGE = "order-history";
  private static final String CHECKOUT_PAGE = "checkout";
  private static final String ORDER_STATUS_ATTRIBUTE_NAME = "status";
  private static final String INSUFFICIENT_FUNDS_EXCEPTION_ERROR_CODE = "insufficient.funds.exception";
  private static final int STARTING_PAGE_NUMBER = 1;
  private static final String DEFAULT_SORT_FIELD = "category";
  private static final String DEFAULT_SORT_DIR = ASCENDING_ORDER_SORTING;
  private static final String DEFAULT_CATEGORY = "all";
  private static final String DEFAULT_PAGE_SIZE = "10";
  private static final int DEFAULT_PAGE_SIZE_INTEGER = 10;
  private OrderService orderService;
  private DishService dishService;
  private UserService userService;

  @Value("${google.maps.api-key}")
  private String googleMapsApiKey;

  @Autowired
  public OrderController(OrderService orderService, DishService dishService,
      UserService userService) {
    this.orderService = orderService;
    this.dishService = dishService;
    this.userService = userService;
  }

  private void fillMenuModelWithData(Model model) {
    model.addAttribute(SORT_FIELD_PARAM, DEFAULT_SORT_FIELD);
    model.addAttribute(SORT_DIR_PARAM, DEFAULT_SORT_DIR);
    model.addAttribute(FILTER_CATEGORY_PARAM, DEFAULT_CATEGORY);
    model.addAttribute(PAGE_SIZE_PARAM, DEFAULT_PAGE_SIZE);
    model.addAttribute(DISH_LIST_ATTRIBUTE, dishService.findAllDishesSorted(
        DEFAULT_SORT_FIELD, DEFAULT_SORT_DIR));
  }

  @PostMapping
  public String saveOrder(@Valid @ModelAttribute OrderCreationDto order,
      BindingResult bindingResult,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpSession session, Model model) {

    if (bindingResult.hasErrors()) {
      model.addAttribute("googleApiKey", googleMapsApiKey);
      return CHECKOUT_PAGE;
    }

    Map<Long, Integer> orders = filterItemsWithoutOrders(order);
    order.setDishIdQuantityMap(orders);

    try {
      orderService.saveOrder(order, authenticatedUser);
      userService.updateUserBalanceInSession(session, authenticatedUser.getUserId());
    } catch (InsufficientFundsException e) {
      log.debug(String.format("User with email %s tried to order, but balance was too low.",
          authenticatedUser.getUsername()), e);

      model.addAttribute("googleApiKey", googleMapsApiKey);
      bindingResult.reject(INSUFFICIENT_FUNDS_EXCEPTION_ERROR_CODE);
      return CHECKOUT_PAGE;
    } catch (EntityNotFoundException e) {
      log.info(
          String.format("User with email %s tried to order, but one of items was not found in DB.",
              authenticatedUser.getUsername()), e);
      bindingResult.reject("dish.absent.exception");
      model.addAttribute("googleApiKey", googleMapsApiKey);
      return CHECKOUT_PAGE;
    }

    return REDIRECT_TO_ORDERS_HISTORY_CLEAR;
  }

  @GetMapping("/history")
  public String getUserOrderHistoryDefault(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      Model model) {
    return getUserOrdersPaginated(STARTING_PAGE_NUMBER,
        ORDER_CREATION_TIME_FIELD, DESCENDING_ORDER_SORTING,
        DEFAULT_CATEGORY, DEFAULT_PAGE_SIZE_INTEGER, authenticatedUser, model);
  }

  @GetMapping("/history/page/{pageNo}")
  public String getUserOrdersPaginated(
      @PathVariable(value = PAGE_NUMBER_PARAM, required = false) int pageNo,
      @RequestParam(SORT_FIELD_PARAM) String sortField,
      @RequestParam(SORT_DIR_PARAM) String sortDir,
      @RequestParam(STATUS_PARAM) String statusParam,
      @RequestParam(PAGE_SIZE_PARAM) int pageSize,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      Model model) {

    Paged<OrderResponseDto> pagedOrder = orderService.findPaginatedByUser(pageNo, pageSize,
        sortField, sortDir, statusParam, authenticatedUser);

    model.addAttribute(ORDER_STATUS_ATTRIBUTE_NAME, statusParam);
    model.addAttribute(CURRENT_PAGE_PARAM, pageNo);

    model.addAttribute(SORT_FIELD_PARAM, sortField);
    model.addAttribute(PAGE_SIZE_PARAM, pageSize);
    model.addAttribute(SORT_DIR_PARAM, sortDir);
    model.addAttribute(REVERSE_SORT_DIR_PARAM,
        sortDir.equals(ASCENDING_ORDER_SORTING) ? DESCENDING_ORDER_SORTING
            : ASCENDING_ORDER_SORTING);
    model.addAttribute(ORDER_LIST_ATTRIBUTE, pagedOrder);
    return ORDER_HISTORY_PAGE;
  }

  @GetMapping
  public String getMappingSupport() {
    return REDIRECT_TO_MENU;
  }

  @GetMapping("/checkout")
  public String showCheckoutPage(Model model) {
    // Populate the model with a new order DTO.
    model.addAttribute("orderCreationDto", new OrderCreationDto());
    model.addAttribute("googleApiKey", googleMapsApiKey);
    return "checkout"; // returns checkout.html view
  }
}
