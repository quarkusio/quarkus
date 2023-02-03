package io.quarkus.hibernate.validator.test.devmode;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NewValidator implements ConstraintValidator<NewConstraint, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return false;
    }
}
