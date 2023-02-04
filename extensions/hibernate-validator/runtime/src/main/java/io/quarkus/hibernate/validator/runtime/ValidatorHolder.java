package io.quarkus.hibernate.validator.runtime;

import jakarta.validation.Validator;

import org.hibernate.validator.HibernateValidatorFactory;

public class ValidatorHolder {

    private static HibernateValidatorFactory validatorFactory;

    private static Validator validator;

    static void initialize(HibernateValidatorFactory validatorFactory) {
        ValidatorHolder.validatorFactory = validatorFactory;
        ValidatorHolder.validator = validatorFactory.getValidator();
    }

    static HibernateValidatorFactory getValidatorFactory() {
        return validatorFactory;
    }

    static Validator getValidator() {
        return validator;
    }
}
