package io.quarkus.hibernate.validator.test.devmode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { NewValidator.class })
@Documented
public @interface NewConstraint {

    String message() default "My new constraint message";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
