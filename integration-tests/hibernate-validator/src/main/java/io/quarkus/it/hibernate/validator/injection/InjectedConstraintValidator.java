package io.quarkus.it.hibernate.validator.injection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
