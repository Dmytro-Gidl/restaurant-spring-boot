package com.exampleepam.restaurant.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderValidator implements ConstraintValidator<HasOrder, Map<Long, Integer>> {
    @Override
    public boolean isValid(Map<Long, Integer> orderMap, ConstraintValidatorContext constraintValidatorContext) {
        if (orderMap == null) return false;
        boolean hasOrders = orderMap.values().stream().anyMatch(integer -> integer > 0);
        log.info("Has orders: {} ", hasOrders);
        return hasOrders;

    }
}
