package com.exampleepam.restaurant.filter;

import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

//@Component
public class UserBalanceFilter implements Filter {

    private final UserService userService;

    @Autowired
    public UserBalanceFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) principal;
            BigDecimal userBalance = userService.getUserBalance(authenticatedUser.getUserId());
            req.setAttribute("userBalance", userBalance);
        }

        filterChain.doFilter(req, res);

    }
}
