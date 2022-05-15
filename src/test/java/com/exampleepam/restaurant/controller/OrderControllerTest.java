package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.ControllerConfiguration;
import com.exampleepam.restaurant.dto.DishResponseDto;
import com.exampleepam.restaurant.entity.paging.Paged;
import com.exampleepam.restaurant.entity.paging.Paging;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.security.MyUserDetailsService;
import com.exampleepam.restaurant.service.DishService;
import com.exampleepam.restaurant.service.OrderService;
import com.exampleepam.restaurant.service.UserService;
import com.exampleepam.restaurant.test_data.TestData;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.ArrayList;

import static com.exampleepam.restaurant.test_data.TestData.*;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(ControllerConfiguration.class)
@WithUserDetails(USER_EMAIL)
public class OrderControllerTest {

    MockMvc mockMvc;
    @MockBean
    OrderService orderService;
    @MockBean
    UserService userService;
    @MockBean
    DishService dishService;
    @MockBean
    MyUserDetailsService myUserDetailsService;
    @MockBean
    Model model;

    @Autowired
    public OrderControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void saveOrder() throws Exception {

        var orderCreation = TestData.getOrderCreationDTO();

        var mockRequest =
                post("/orders")
                        .flashAttr("orderCreationDto", orderCreation)
                        .with(csrf());

        mockMvc.perform(mockRequest)
                .andExpect(MockMvcResultMatchers.redirectedUrl("/orders/history"));
        AuthenticatedUser authenticatedUser = getBasicUserDetails();
        Mockito.verify(orderService, times(1)).saveOrder(orderCreation, authenticatedUser);
    }

    @Test
    public void getUserOrdersPaginated() throws Exception {
        String sortField = "category";
        String sortDir = "asc";
        String status = "all";
        int pageSize = 1;
        int pageNo = 2;
        var mockRequest =
                MockMvcRequestBuilders.get("/orders/history/page/2")
                        .param("sortField", sortField)
                        .param("sortDir", sortDir)
                        .param("status", status)
                        .param("pageSize", String.valueOf(pageSize))
                        .with(csrf());

        AuthenticatedUser basicUserDetails = getBasicUserDetails();
        Mockito.when(orderService.findPaginatedByUser(pageNo, pageSize, sortField, sortDir,
                status, basicUserDetails))
                .thenReturn(getOrderResponseDtosPaged());
        Mockito.when(userService.getUserBalance(1)).thenReturn(BigDecimal.valueOf(2321));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("order-history"))
                .andExpect(model().attributeExists("sortDir",
                        "status", "pageSize", "currentPage", "reverseSortDir",
                        "orderList"));
    }

    @Test
    public void getMappingSupport() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/orders")
                        .with(csrf()))
                .andExpect(MockMvcResultMatchers.redirectedUrl("/menu"));
    }


}
