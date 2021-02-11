package io.quarkus.hibernate.validator.runtime;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

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
