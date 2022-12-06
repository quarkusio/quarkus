package io.quarkus.hibernate.validator.runtime;

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class ValidatorHolder {

    private static ValidatorFactory validatorFactory;

    private static Validator validator;

    static void initialize(ValidatorFactory validatorFactory) {
        ValidatorHolder.validatorFactory = validatorFactory;
        ValidatorHolder.validator = validatorFactory.getValidator();
    }

    static ValidatorFactory getValidatorFactory() {
        return validatorFactory;
    }

    static Validator getValidator() {
        return validator;
    }
}
