package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.ControllerConfiguration;
import com.exampleepam.restaurant.security.MyUserDetailsService;
import com.exampleepam.restaurant.service.DishService;
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
import org.springframework.ui.Model;

import java.math.BigDecimal;

import static com.exampleepam.restaurant.test_data.TestData.USER_EMAIL;
import static com.exampleepam.restaurant.test_data.TestData.getDishResponseDtos;
import static com.exampleepam.restaurant.test_data.TestData.getDishResponseDtosPaged;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DishController.class)
@Import(ControllerConfiguration.class)
@WithUserDetails(USER_EMAIL)
public class DishControllerTest {

    MockMvc mockMvc;
    @MockBean
    DishService dishService;
    @MockBean
    UserService userService;
    @MockBean
    MyUserDetailsService myUserDetailsService;
    @MockBean
    Model model;

    @Autowired
    public DishControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    public void returnMenuSorted() throws Exception {
        String sortField = "name";
        String sortDir = "asc";
        String filterCategory = "all";
        int pageNo = 1;
        int pageSize = 6;

        var mockRequest =
                MockMvcRequestBuilders.get("/menu")
                        .param("sortField", sortField)
                        .param("sortDir", sortDir)
                        .param("filterCategory", filterCategory)
                        .param("pageNo", String.valueOf(pageNo))
                        .param("pageSize", String.valueOf(pageSize))
                        .with(csrf());

        Mockito.when(dishService.findPaginated(pageNo, pageSize, sortField, sortDir, filterCategory))
                .thenReturn(getDishResponseDtosPaged());
        Mockito.when(userService.getUserBalance(1)).thenReturn(BigDecimal.valueOf(2321));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("menu"))
                .andExpect(model().attributeExists("filterCategory", "sortDir","dishList",
                         "reverseSortDir", "currentPage", "pageSize"));
    }

}
