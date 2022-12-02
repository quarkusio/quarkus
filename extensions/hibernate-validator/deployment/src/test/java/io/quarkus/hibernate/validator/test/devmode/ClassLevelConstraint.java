package io.quarkus.hibernate.validator.test.devmode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { ClassLevelValidator.class })
@Documented
public @interface ClassLevelConstraint {

    String message() default "My class constraint message";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
