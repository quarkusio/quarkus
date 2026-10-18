package io.quarkus.hibernate.validator.runtime;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidatorFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

public final class ValidationSupport {

    private ValidationSupport() {
    }

    @SuppressWarnings("unused") // this is called by transformed code
    public static ValidatorFactory buildDefaultValidatorFactory() {
        ArcContainer container = Arc.container();
        if (container == null) {
            return fallback();
        }

        InstanceHandle<HibernateValidatorFactory> instance = container.instance(HibernateValidatorFactory.class);
        if (!instance.isAvailable()) {
            return fallback();
        }

        return new CloseAsNoopValidatorFactoryWrapper(instance.get());
    }

    // the point of having this is to support non-Quarkus tests that could be using Hibernate Validator
    private static ValidatorFactory fallback() {
        return Validation.byDefaultProvider().configure().buildValidatorFactory();
    }
}
