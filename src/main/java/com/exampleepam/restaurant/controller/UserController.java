package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.UserCreationDto;
import com.exampleepam.restaurant.exception.UserAlreadyExistAuthenticationException;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * User Controller
 */
@Slf4j
@Controller
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    public static final String SIGN_UP_PAGE = "sign-up";

    @GetMapping("/signup")
    public String register(Model model) {
        model.addAttribute("user", new UserCreationDto());
        return SIGN_UP_PAGE;
    }


    @PostMapping("/signup")
    public String registration(@ModelAttribute("user") @Valid UserCreationDto userCreationDto,
                               BindingResult bindingResult,
                               Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("registrationForm", userCreationDto);
            return SIGN_UP_PAGE;
        }

        try {
            userService.register(userCreationDto);
            log.debug(String.format("User has registred with email %s.", userCreationDto.getEmail()));
        } catch (UserAlreadyExistAuthenticationException e) {
            model.addAttribute("registrationForm", userCreationDto);
            bindingResult.rejectValue("email",
                    "fail.account.exists", "An account already exists for this email.");

            log.info(String.format("User tried to registr with email %s. But account already exists."
                    , userCreationDto.getEmail()), e);
            return SIGN_UP_PAGE;
        }

        return "redirect:/menu";
    }

    @PutMapping("/topup")
    public String toptupBalance(@RequestParam("balance") BigDecimal balance,
                                @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {

        long userId = authenticatedUser.getUserId();
        userService.addUserBalance(userId, balance);
        log.info(String.format("User has topped up balance for %s UAH.", balance));
        return "redirect:/menu";
    }

}
