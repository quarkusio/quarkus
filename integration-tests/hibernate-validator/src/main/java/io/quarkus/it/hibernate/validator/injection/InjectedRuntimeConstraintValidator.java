package io.quarkus.it.hibernate.validator.injection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@ApplicationScoped
public class InjectedRuntimeConstraintValidator
        implements ConstraintValidator<InjectedRuntimeConstraintValidatorConstraint, String> {

    @Inject
    Instance<MyRuntimeService> service;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return service.get().validate(value);
    }
}
