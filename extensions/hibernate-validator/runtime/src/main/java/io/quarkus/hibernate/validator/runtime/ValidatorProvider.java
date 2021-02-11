package io.quarkus.hibernate.validator.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

@ApplicationScoped
public class ValidatorProvider {

    @Produces
    @Named("quarkus-hibernate-validator-factory")
    public ValidatorFactory factory() {
        return ValidatorHolder.getValidatorFactory();
    }

    @Produces
    public Validator validator() {
        return ValidatorHolder.getValidator();
    }
}
