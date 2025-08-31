package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.controller.BaseController;
import com.exampleepam.restaurant.dto.order.OrderResponseDto;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.exception.UnauthorizedActionException;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.OrderService;
import com.exampleepam.restaurant.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpSession;

/**
 * Order Controller for Admins
 */
@Slf4j
@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController extends BaseController {

    private static final String ORDER_MANAGEMENT_PAGE = "order-management";
    private static final String REDIRECT_TO_ADMIN_ORDERS = "redirect:/admin/orders";
    private static final String DEFAULT_STATUS = "active";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String ORDER_ID_PARAM = "orderId";
    private static final String ACTION_PARAM = "action";
    private static final int DEFAULT_PAGE_NUMBER = 1;
    private final OrderService orderService;
    private final UserService userService;

    @Autowired
    public AdminOrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    private static final String STATUS_ACTION_DECLINE = "decline";
    private static final String STATUS_ACTION_NEXT = "next";


    @GetMapping
    public String getOrdersDefault(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                   Model model) {
        return findPaginated(DEFAULT_PAGE_NUMBER,
                ORDER_CREATION_TIME_FIELD, DESCENDING_ORDER_SORTING, DEFAULT_STATUS, DEFAULT_PAGE_SIZE, authenticatedUser, model);
    }

    @DeleteMapping("/{id}")
    public String deleteOrder(@PathVariable(value = "id") long id) {
        try {
            orderService.delete(id);
            log.debug(String.format("Order with id %d was deleted", id));
        } catch (EntityNotFoundException e) {
            log.debug("Admin tried to delete order with id %d, but failed. Order was not found in DB", e);
        }
        return REDIRECT_TO_ADMIN_ORDERS;
    }


    @GetMapping("/page/{pageNo}")
    public String findPaginated(@PathVariable(value = PAGE_NUMBER_PARAM) int pageNo,
                                @RequestParam(SORT_FIELD_PARAM) String sortField,
                                @RequestParam(SORT_DIR_PARAM) String sortDir,
                                @RequestParam(STATUS_PARAM) String statusParam,
                                @RequestParam(PAGE_SIZE_PARAM) int pageSize,
                                @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                Model model) {

        Paged<OrderResponseDto> pagedOrder = orderService.findPaginated(pageNo, pageSize,
                sortField, sortDir, statusParam);

        model.addAttribute(STATUS_PARAM, statusParam);
        model.addAttribute(CURRENT_PAGE_PARAM, pageNo);

        model.addAttribute(SORT_FIELD_PARAM, sortField);
        model.addAttribute(PAGE_SIZE_PARAM, pageSize);
        model.addAttribute(SORT_DIR_PARAM, sortDir);
        model.addAttribute(REVERSE_SORT_DIR_PARAM,
                sortDir.equals(ASCENDING_ORDER_SORTING) ? DESCENDING_ORDER_SORTING : ASCENDING_ORDER_SORTING);

        model.addAttribute(ORDER_LIST_ATTRIBUTE, pagedOrder);

        return ORDER_MANAGEMENT_PAGE;
    }

    @PutMapping(value = "/{orderId}/{action}/page/{pageNo}")
    public String changeOrderStatus(@PathVariable(value = ACTION_PARAM) String action,
                                    @PathVariable(value = ORDER_ID_PARAM) Long orderId,
                                    @PathVariable(value = PAGE_NUMBER_PARAM) int pageNo,
                                    @RequestParam(SORT_FIELD_PARAM) String sortField,
                                    @RequestParam(SORT_DIR_PARAM) String sortDir,
                                    @RequestParam(STATUS_PARAM) String statusParam,
                                    @RequestParam(PAGE_SIZE_PARAM) int pageSize,
                                    HttpSession session) {

        if (action.equals(STATUS_ACTION_DECLINE)) {
            try {
                orderService.setStatusDeclinedAndRefund(orderId);
                long userId = orderService.getUserIdByOrderId(orderId);
                userService.updateUserBalanceInSession(session, userId);
                log.debug("Status for order with id {} was set to DECLINED", orderId);
            } catch (EntityNotFoundException e) {
                log.warn("Admin tried to change status, but order with id {} was not found in DB", orderId);
            }
        }

        if (action.equals(STATUS_ACTION_NEXT)) {
            try {
                orderService.setNextStatus(orderId);
                log.info("Status for order with id {} was set to next", orderId);
            } catch (UnauthorizedActionException e) {
                log.warn("Admin tried to change status for DECLINED or COMPLETED order", e);
            } catch (EntityNotFoundException e) {
                log.warn("Admin tried to change status, but order with id {} was not found in DB", orderId);
            }
        }

        String redirectLink = UriComponentsBuilder.fromPath("/admin/orders/page/{pageNo}")
                .queryParam(SORT_FIELD_PARAM, sortField)
                .queryParam(SORT_DIR_PARAM, sortDir)
                .queryParam(STATUS_PARAM, statusParam)
                .queryParam(PAGE_SIZE_PARAM, pageSize)
                .buildAndExpand(pageNo)
                .toUriString();
        return "redirect:" + redirectLink;

    }
}




