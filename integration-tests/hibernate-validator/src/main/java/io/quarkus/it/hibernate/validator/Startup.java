package io.quarkus.it.hibernate.validator;

import java.util.Objects;

import jakarta.enterprise.event.Observes;
import jakarta.validation.Validation;

import io.quarkus.runtime.StartupEvent;

public class Startup {

    public void onStart(@Observes StartupEvent ev) {
        Objects.requireNonNull(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
