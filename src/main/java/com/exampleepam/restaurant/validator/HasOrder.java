package com.exampleepam.restaurant.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = OrderValidator.class)
@Documented
public @interface HasOrder {
    String message() default "Please, order something.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

