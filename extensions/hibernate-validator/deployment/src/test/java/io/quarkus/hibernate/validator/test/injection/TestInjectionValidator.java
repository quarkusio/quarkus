package io.quarkus.hibernate.validator.test.injection;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TestInjectionValidator implements ConstraintValidator<TestConstraint, String> {

    @Inject
    TestInjectedBean testInjectedBean;

    @Override
    public boolean isValid(String string, ConstraintValidatorContext context) {
        return testInjectedBean.allowedStrings().contains(string);
    }

}
