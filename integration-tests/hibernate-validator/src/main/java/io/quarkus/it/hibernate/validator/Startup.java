package io.quarkus.it.hibernate.validator;

import java.util.Objects;

import javax.enterprise.event.Observes;
import javax.validation.Validation;

import io.quarkus.runtime.StartupEvent;

public class Startup {

    public void onStart(@Observes StartupEvent ev) {
        Objects.requireNonNull(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
