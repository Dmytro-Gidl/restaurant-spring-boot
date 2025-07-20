package com.exampleepam.restaurant.validator;

import com.exampleepam.restaurant.dto.user.UserCreationDto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, UserCreationDto> {

    @Override
    public boolean isValid(final UserCreationDto user, final ConstraintValidatorContext context) {
        return user.getPassword().equals(user.getMatchingPassword());
    }
}
