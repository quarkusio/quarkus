package io.quarkus.hibernate.validator.test.injection;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TestInjectionValidator implements ConstraintValidator<TestConstraint, String> {

    @Inject
    TestInjectedBean testInjectedBean;

    @Override
    public boolean isValid(String string, ConstraintValidatorContext context) {
        return testInjectedBean.allowedStrings().contains(string);
    }

}
