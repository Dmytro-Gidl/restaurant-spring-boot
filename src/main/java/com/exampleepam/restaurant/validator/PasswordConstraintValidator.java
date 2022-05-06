package com.exampleepam.restaurant.validator;

import org.passay.*;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;


public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {
    @Override
    public boolean isValid(final String password, final ConstraintValidatorContext context) {
        // @formatter:off
        final PasswordValidator validator = new PasswordValidator(Arrays.asList(
                new UsernameRule(),
                new LengthRule(8, 30),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new IllegalSequenceRule(EnglishSequenceData.USQwerty, 5, false),
                new WhitespaceRule()));

        final RuleResult result = validator.validate(new PasswordData(password));
        if (!result.isValid()) {
            List<RuleResultDetail> resultList = result.getDetails();
            context.disableDefaultConstraintViolation();

            boolean sizeFail = resultList.stream()
                    .flatMap(ruleResultDetail -> Arrays.stream(ruleResultDetail.getErrorCodes()))
                    .anyMatch(element -> element.equals("TOO_SHORT") || element.equals("TOO_LONG"));

            if (sizeFail) {
                context.buildConstraintViolationWithTemplate("{fail.validation.length.password}").addConstraintViolation();
            } else {
                context.buildConstraintViolationWithTemplate("{fail.validation.symbol.password}").addConstraintViolation();
            }
            return false;
        }
        return true;


    }
}
