package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.ControllerConfiguration;
import com.exampleepam.restaurant.security.MyUserDetailsService;
import com.exampleepam.restaurant.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static com.exampleepam.restaurant.test_data.TestData.USER_EMAIL;
import static com.exampleepam.restaurant.test_data.TestData.getUserCreationDto;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;


@WebMvcTest(UserController.class)
@Import(ControllerConfiguration.class)
class UserControllerTest {
    MockMvc mockMvc;

    @MockBean
    UserService userService;
    @MockBean
    MyUserDetailsService myUserDetailsService;

    @Autowired
    public UserControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createUser() throws Exception {

        var userCreation = getUserCreationDto();

        var mockRequest =
                post("/signup")
                        .flashAttr("user", userCreation)
                        .with(csrf());

        mockMvc.perform(mockRequest)
                .andExpect(status().is3xxRedirection());
        Mockito.verify(userService, times(1)).register(userCreation);
    }

    @Test
    public void registration() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/signup")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("sign-up"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithUserDetails(USER_EMAIL)
    public void topupBalance() throws Exception {
        mockMvc.perform(put("/topup")
                        .param("balance", "200")
                        .with(csrf()))
                .andExpect(MockMvcResultMatchers.redirectedUrl("/menu"));
    }


}
