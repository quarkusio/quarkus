package io.quarkus.hibernate.validator.test.devmode;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NewValidator implements ConstraintValidator<NewConstraint, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return false;
    }
}
