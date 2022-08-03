package io.quarkus.it.smallrye.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

@Target({ ElementType.TYPE_USE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Prod.Validator.class)
public @interface Prod {
    String message() default "server is not prod";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<Prod, Cloud> {
        @Override
        public boolean isValid(final Cloud value, final ConstraintValidatorContext context) {
            return value.server().equals("prod");
        }
    }
}
