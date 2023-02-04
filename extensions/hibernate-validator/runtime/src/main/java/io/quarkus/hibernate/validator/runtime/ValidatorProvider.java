package io.quarkus.hibernate.validator.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.Validator;

import org.hibernate.validator.HibernateValidatorFactory;

@Singleton
public class ValidatorProvider {

    @Produces
    @Named("quarkus-hibernate-validator-factory")
    @Singleton
    public HibernateValidatorFactory factory() {
        return ValidatorHolder.getValidatorFactory();
    }

    @Produces
    @Singleton
    public Validator validator() {
        return ValidatorHolder.getValidator();
    }
}
