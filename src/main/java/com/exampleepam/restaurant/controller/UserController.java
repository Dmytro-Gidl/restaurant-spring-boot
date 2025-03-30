package com.exampleepam.restaurant.controller;

import com.exampleepam.restaurant.dto.user.UserCreationDto;
import com.exampleepam.restaurant.exception.UserAlreadyExistAuthenticationException;
import com.exampleepam.restaurant.security.AuthenticatedUser;
import com.exampleepam.restaurant.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * User Controller
 */
@Slf4j
@Controller
public class UserController extends BaseController {

    private static final String SIGN_UP_PAGE = "sign-up";
    private static final String USER_ATTRIBUTE = "user";
    private static final String REGISTRATION_FORM_ATTRIBUTE = "registrationForm";
    private static final String BALANCE_PARAMETER = "balance";
    private static final String ACCOUNT_EXISTS_ERROR_CODE = "fail.account.exists";
    private static final String ACCOUNT_EXISTS_DEFAULT_MESSAGE = "An account already exists for this email.";
    private static final String EMAIL_FIELD = "email";
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/signup")
    public String register(Model model) {
        model.addAttribute(USER_ATTRIBUTE, new UserCreationDto());
        return SIGN_UP_PAGE;
    }

    @PostMapping("/signup")
    public String registration(@ModelAttribute(USER_ATTRIBUTE) @Valid UserCreationDto userCreationDto,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(REGISTRATION_FORM_ATTRIBUTE, userCreationDto);
            return SIGN_UP_PAGE;
        }

        try {
            userService.register(userCreationDto);
            log.debug("User has registered with email {}.", userCreationDto.getEmail());
        } catch (UserAlreadyExistAuthenticationException e) {
            model.addAttribute(REGISTRATION_FORM_ATTRIBUTE, userCreationDto);
            bindingResult.rejectValue(EMAIL_FIELD,
                    ACCOUNT_EXISTS_ERROR_CODE, ACCOUNT_EXISTS_DEFAULT_MESSAGE);

            log.info("User tried to register with email {}. But account already exists."
                    , userCreationDto.getEmail(), e);
            return SIGN_UP_PAGE;
        }
        return REDIRECT_TO_MENU;
    }

    @PutMapping("/topup")
    public String topupBalance(@RequestParam(BALANCE_PARAMETER) BigDecimal balance,
                               @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                               HttpSession session) {
        long userId = authenticatedUser.getUserId();
        userService.addUserBalance(userId, balance);
        userService.updateUserBalanceInSession(session, userId);
        log.info("User {} has topped up balance for {} UAH.", userId, balance);
        return REDIRECT_TO_MENU;
    }
}
