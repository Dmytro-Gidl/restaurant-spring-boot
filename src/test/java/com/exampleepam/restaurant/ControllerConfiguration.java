package com.exampleepam.restaurant;

import com.exampleepam.restaurant.entity.User;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.security.CustomAuthenticationSuccessHandler;
import com.exampleepam.restaurant.service.UserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.exampleepam.restaurant.test_data.TestData.getAdminUser;
import static com.exampleepam.restaurant.test_data.TestData.getBasicUser;

@TestConfiguration
public class ControllerConfiguration {

    @Bean
    @Primary
    public UserDetailsService myUserDetailsService() {
        var basicUser = getBasicUser();
        var adminUser = getAdminUser();
        return s -> {
            List<User> list = new ArrayList<>(Arrays.asList(basicUser, adminUser));
            return new AuthenticatedUser(
                    list.stream()
                            .filter(u -> u.getEmail().equals(s)).findAny()
                            .orElse(basicUser));
        };
    }

    @Bean
    @Primary
    public CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler(UserService userService) {
        return new CustomAuthenticationSuccessHandler(userService);
    }

}
