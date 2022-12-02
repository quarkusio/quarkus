package io.quarkus.it.hibernate.validator.injection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@ApplicationScoped
public class InjectedConstraintValidator implements ConstraintValidator<InjectedConstraintValidatorConstraint, String> {

    @Inject
    MyService service;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return service.validate(value);
    }
}
