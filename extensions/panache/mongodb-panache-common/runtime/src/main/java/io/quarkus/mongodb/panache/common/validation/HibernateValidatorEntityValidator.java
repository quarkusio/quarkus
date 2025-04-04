package io.quarkus.mongodb.panache.common.validation;

import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

public class HibernateValidatorEntityValidator
        implements EntityValidator<ConstraintViolation<?>, ConstraintViolationException> {

    private final Validator validator;

    public HibernateValidatorEntityValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Set<? extends ConstraintViolation<?>> validate(Object entity) {
        return validator.validate(entity);
    }

    @Override
    public Optional<ConstraintViolationException> toException(Set<? extends ConstraintViolation<?>> violations) {
        if (violations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ConstraintViolationException(violations));
    }
}
