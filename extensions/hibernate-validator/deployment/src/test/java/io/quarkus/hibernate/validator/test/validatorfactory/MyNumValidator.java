package io.quarkus.hibernate.validator.test.validatorfactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.Min;

public class MyNumValidator implements ConstraintValidator<Min, Integer> {
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value >= 0;
    }
}