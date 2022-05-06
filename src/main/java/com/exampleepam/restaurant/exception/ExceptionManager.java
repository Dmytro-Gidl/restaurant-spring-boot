package com.exampleepam.restaurant.exception;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;

/**
 * Helper class for getting exceptions
 */
public class ExceptionManager {

    private ExceptionManager() {
    }

    public static EntityNotFoundException getNotFoundException(EntityType entityType, long id) {
        String eMessage = String
                .format("Entity %s with id %d was not found in DB", entityType, id);
        return new EntityNotFoundException(eMessage);
    }

    public static InsufficientFundsException getInsufficientFundsException(long userId, BigDecimal userBalance,
                                                                           BigDecimal orderTotalSum) {
        String eMessage = String
                .format("User with Id %d tried to order dishes for %s but had only %s",
                        userId, orderTotalSum, userBalance);
        return new InsufficientFundsException(eMessage);
    }

    public static UserAlreadyExistAuthenticationException getUserAlreadyExistsException(
            String email) {

        String eMessage = String
                .format("User with email %s tried to register, but account already exists", email);
        return new UserAlreadyExistAuthenticationException(eMessage);
    }
    public static UnauthorizedActionException getUnauthorizedActionException(String eMessage) {

        return new UnauthorizedActionException(eMessage);
    }
}
