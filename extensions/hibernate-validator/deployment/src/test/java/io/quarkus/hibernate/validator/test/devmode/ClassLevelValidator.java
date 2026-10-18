package io.quarkus.hibernate.validator.test.devmode;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ClassLevelValidator implements ConstraintValidator<ClassLevelConstraint, TestBean> {
    @Override
    public boolean isValid(TestBean bean, ConstraintValidatorContext context) {
        return false;
    }
}
