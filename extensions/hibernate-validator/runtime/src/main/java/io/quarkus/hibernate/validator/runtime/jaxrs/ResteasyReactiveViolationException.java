package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * Wrapper around a {@link javax.validation.ConstraintViolationException},
 * used to mark a constraint violation as relative to a REST endpoint call.
 * <p>
 * Those violations are handled differently than violations from other, internal components:
 * a violation on an internal component is always considered an internal error (HTTP 500),
 * while a violation on the parameters of a REST endpoint call is a client error (HTTP 400).
 */
public class ResteasyReactiveViolationException extends ConstraintViolationException {
    private static final long serialVersionUID = 657697354453281559L;

    public ResteasyReactiveViolationException(String message, Set<? extends ConstraintViolation<?>> constraintViolations) {
        super(message, constraintViolations);
    }

    public ResteasyReactiveViolationException(Set<? extends ConstraintViolation<?>> constraintViolations) {
        super(constraintViolations);
    }
}
