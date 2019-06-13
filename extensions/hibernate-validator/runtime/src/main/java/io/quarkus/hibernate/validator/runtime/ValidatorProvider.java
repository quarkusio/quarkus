package io.quarkus.hibernate.validator.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

@ApplicationScoped
public class ValidatorProvider {

    @Produces
    public ValidatorFactory factory() {
        return ValidatorHolder.getValidatorFactory();
    }

    @Produces
    public Validator validator() {
        return ValidatorHolder.getValidator();
    }
}
