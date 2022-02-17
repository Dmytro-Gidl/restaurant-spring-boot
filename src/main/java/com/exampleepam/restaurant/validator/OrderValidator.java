package com.exampleepam.restaurant.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashMap;
import java.util.Map;

public class OrderValidator implements ConstraintValidator<HasOrder, Map<Long, Integer>>{
    @Override
    public boolean isValid(Map<Long, Integer> orderMap, ConstraintValidatorContext constraintValidatorContext) {
        if(orderMap == null) return false;
        return orderMap.values().stream().anyMatch(integer -> integer > 0);

    }
}
