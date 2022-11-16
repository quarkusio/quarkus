package io.quarkus.hibernate.validator.runtime;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

public final class ValidationSupport {

    private ValidationSupport() {
    }

    @SuppressWarnings("unused")
    public static ValidatorFactory buildDefaultValidatorFactory() {
        ArcContainer container = Arc.container();
        if (container == null) {
            return fallback();
        }

        InstanceHandle<ValidatorFactory> instance = container.instance(ValidatorFactory.class);
        if (!instance.isAvailable()) {
            return fallback();
        }

        return instance.get();
    }

    // the point of having this is to support non-Quarkus tests that could be using Hibernate Validator
    private static ValidatorFactory fallback() {
        return Validation.byDefaultProvider().configure().buildValidatorFactory();
    }
}
