package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.dto.OrderResponseDto;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.exception.UnauthorizedActionException;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityNotFoundException;

/**
 * Order Controller for Admins
 */
@Slf4j
@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {
    private OrderService orderService;

    @Autowired
    public AdminOrderController(OrderService orderService ) {
        this.orderService = orderService;
    }

    private static final String STATUS_ACTION_DECLINE = "decline";
    private static final String STATUS_ACTION_NEXT = "next";


    @GetMapping
    public String getOrdersDefault(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                   Model model) {
        return findPaginated(1,
                "creationDateTime", "desc", "all", 10, authenticatedUser, model);
    }


    @DeleteMapping("/{id}")
    public String deleteOrder(@PathVariable(value = "id") long id) {
        try {
            orderService.delete(id);
            log.debug(String.format("Order with id %d was deleted", id));
        } catch (EntityNotFoundException e) {
            log.debug("Admin tried to delete order with id %d, but failed. Order was not found in DB", e);
        }
        return "redirect:/admin/orders";
    }


    @GetMapping("/page/{pageNo}")
    public String findPaginated(@PathVariable(value = "pageNo") int pageNo,
                                @RequestParam("sortField") String sortField,
                                @RequestParam("sortDir") String sortDir,
                                @RequestParam("status") String statusParam,
                                @RequestParam("pageSize") int pageSize,
                                @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                Model model) {

        Paged<OrderResponseDto> pagedOrder = orderService.findPaginated(pageNo, pageSize,
                sortField, sortDir, statusParam);

        model.addAttribute("status", statusParam);
        model.addAttribute("currentPage", pageNo);

        model.addAttribute("sortField", sortField);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        model.addAttribute("orderList", pagedOrder);

        return "order-management";
    }

    @PutMapping(value = "/{orderId}/{action}/page/{pageNo}")
    public String changeOrderStatus(@PathVariable(value = "action") String action,
                                    @PathVariable(value = "orderId") Long orderId,
                                    @PathVariable(value = "pageNo") int pageNo,
                                    @RequestParam("sortField") String sortField,
                                    @RequestParam("sortDir") String sortDir,
                                    @RequestParam("status") String statusParam,
                                    @RequestParam("pageSize") int pageSize
    ) {

        if (action.equals(STATUS_ACTION_DECLINE)) {
            try {
                orderService.setStatusDeclinedAndRefund(orderId);
                log.debug(String.format("Status for order with id %d was set to DECLINED", orderId));
            } catch (EntityNotFoundException e) {
                log.warn(String.format("Admin tried to change status, but order with id %d was not found in DB", orderId));
            }
        }

        if(action.equals(STATUS_ACTION_NEXT)) {
            try {
                orderService.setNextStatus(orderId);
                log.info(String.format("Status for order with id %d was set to next", orderId));
            } catch (UnauthorizedActionException e) {
                log.warn("Admin tried to change status for DECLINED or COMPLETED order", e);
            } catch (EntityNotFoundException e) {
                log.warn(String.format("Admin tried to change status, but order with id %d was not found in DB", orderId));
            }
        }

        String redirectLink = UriComponentsBuilder.fromPath("/admin/orders/page/{pageNo}")
                .queryParam("sortField", sortField)
                .queryParam("sortDir", sortDir)
                .queryParam("status", statusParam)
                .queryParam("pageSize", pageSize)
                .buildAndExpand(pageNo)
                .toUriString();
        return"redirect:"+redirectLink;

    }
}




