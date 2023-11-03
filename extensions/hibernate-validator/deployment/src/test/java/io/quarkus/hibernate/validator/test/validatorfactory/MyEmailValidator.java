package io.quarkus.hibernate.validator.test.validatorfactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Email;

public class MyEmailValidator implements ConstraintValidator<Email, CharSequence> {
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return "test1234@acme.com".contentEquals(value);
    }
}
