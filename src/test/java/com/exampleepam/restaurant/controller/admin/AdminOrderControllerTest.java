package com.exampleepam.restaurant.controller.admin;

import com.exampleepam.restaurant.ControllerConfiguration;
import com.exampleepam.restaurant.controller.DishController;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.security.MyUserDetailsService;
import com.exampleepam.restaurant.service.OrderService;
import com.exampleepam.restaurant.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.persistence.EntityNotFoundException;

import java.math.BigDecimal;

import static com.exampleepam.restaurant.test_data.TestData.*;
import static com.exampleepam.restaurant.test_data.TestData.getOrderResponseDtosPaged;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminOrderController.class)
@Import(ControllerConfiguration.class)
@WithUserDetails(ADMIN_EMAIL)
public class AdminOrderControllerTest {
    MockMvc mockMvc;

    @MockBean
    private OrderService orderService;
    @MockBean
    private UserService userService;
    @MockBean
    MyUserDetailsService myUserDetailsService;

    @Autowired
    public AdminOrderControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void deleteOrder() throws Exception {
                mockMvc.perform(MockMvcRequestBuilders.delete("/admin/orders/5")
                        .with(csrf()));
        Mockito.verify(orderService, Mockito.times(1)).deleteOrderById(5);
    }
    @Test
     void findPaginated() throws Exception {
        String sortField = "category";
        String sortDir = "asc";
        String status = "all";
        int pageSize = 4;
        int pageNo = 4;
        var mockRequest =
                MockMvcRequestBuilders.get("/admin/orders/page/4")
                        .param("sortField", sortField)
                        .param("sortDir", sortDir)
                        .param("status", status)
                        .param("pageSize", String.valueOf(pageSize))
                        .with(csrf());

        Mockito.when(orderService.findPaginated(pageNo, pageSize, sortField, sortDir,
                        status))
                .thenReturn(getOrderResponseDtosPaged());
        Mockito.when(userService.getUserBalance(2)).thenReturn(BigDecimal.valueOf(2321));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("order-management"))
                .andExpect(model().attributeExists("sortDir",
                        "status", "pageSize", "currentPage", "reverseSortDir",
                        "orderList", "userBalance"));
    }

    @Test
    public void changeOrderStatusNext() throws Exception {
        String sortField = "category";
        String sortDir = "asc";
        String status = "all";
        int pageSize = 4;
        var mockRequest =
                MockMvcRequestBuilders.put("/admin/orders/15/next/page/2")
                        .param("sortField", sortField)
                        .param("sortDir", sortDir)
                        .param("status", status)
                        .param("pageSize", String.valueOf(pageSize))
                        .with(csrf());
        mockMvc.perform(mockRequest)
                        .andExpect(status().is3xxRedirection());
        Mockito.verify(orderService, Mockito.times(1)).setNextStatus(15L);

    }
    @Test
    public void changeOrderStatusDecline() throws Exception {
        String sortField = "category";
        String sortDir = "asc";
        String status = "all";
        int pageSize = 4;
        var mockRequest =
                MockMvcRequestBuilders.put("/admin/orders/15/decline/page/2")
                        .param("sortField", sortField)
                        .param("sortDir", sortDir)
                        .param("status", status)
                        .param("pageSize", String.valueOf(pageSize))
                        .with(csrf());
        mockMvc.perform(mockRequest)
                .andExpect(status().is3xxRedirection());
        Mockito.verify(orderService, Mockito.times(1)).setStatusDeclinedAndRefund(15L);

    }
}

