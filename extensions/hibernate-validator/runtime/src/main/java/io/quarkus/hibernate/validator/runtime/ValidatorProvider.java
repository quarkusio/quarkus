package io.quarkus.hibernate.validator.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

@Singleton
public class ValidatorProvider {

    @Produces
    @Named("quarkus-hibernate-validator-factory")
    @Singleton
    public ValidatorFactory factory() {
        return ValidatorHolder.getValidatorFactory();
    }

    @Produces
    @Singleton
    public Validator validator() {
        return ValidatorHolder.getValidator();
    }
}
