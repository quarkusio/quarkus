package io.quarkus.it.hibernate.validator.custom;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MyServiceLoadedConstraintValidator implements ConstraintValidator<MyServiceLoadedConstraint, MyOtherBean> {

    @Override
    public boolean isValid(MyOtherBean value, ConstraintValidatorContext context) {
        if (value == null || value.getName() == null) {
            return true;
        }
        return !"fail".equalsIgnoreCase(value.getName());
    }
}
