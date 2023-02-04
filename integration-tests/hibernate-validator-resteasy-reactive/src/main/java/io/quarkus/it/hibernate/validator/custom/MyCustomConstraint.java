package io.quarkus.it.hibernate.validator.custom;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

@Target({ ElementType.TYPE_USE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MyCustomConstraint.Validator.class)
public @interface MyCustomConstraint {

    String message() default "{MyCustomConstraint.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<MyCustomConstraint, MyOtherBean> {

        @Override
        public boolean isValid(MyOtherBean value, ConstraintValidatorContext context) {
            return value.getName() != null;
        }
    }
}
