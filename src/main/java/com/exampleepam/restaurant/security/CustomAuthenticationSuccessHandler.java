package com.exampleepam.restaurant.security;

import static com.exampleepam.restaurant.controller.BaseController.USER_BALANCE_SESSION_ATTRIBUTE;
import static com.exampleepam.restaurant.controller.BaseController.USER_CART_ITEMS_SESSION_ATTRIBUTE;

import com.exampleepam.restaurant.service.UserService;
import java.io.IOException;
import java.math.BigDecimal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final UserService userService;

  @Autowired
  public CustomAuthenticationSuccessHandler(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
    BigDecimal userBalance = userService.getUserBalance(authenticatedUser.getUserId());
    request.getSession().setAttribute(USER_BALANCE_SESSION_ATTRIBUTE, userBalance);
    response.sendRedirect("/");
  }
}
