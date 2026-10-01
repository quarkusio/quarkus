package io.quarkus.it.hibernate.validator.config;

import jakarta.validation.constraints.Size;

import io.smallrye.config.ConfigMapping;

/**
 * A config mapping with no constraint on the root interface, or on the first level of nesting; the only
 * constraint is two levels deep, on {@link Nested.Deeper#value()}. The whole tree must still be registered
 * for reflection and walked by the validator, even though the root mapping interface itself is unconstrained.
 */
@ConfigMapping(prefix = "config-validation.nested-validated")
public interface NestedValidatedConfig {

    String value();

    Nested nested();

    interface Nested {

        String value();

        Deeper deeper();

        interface Deeper {

            @Size(min = 2, max = 20)
            String value();
        }
    }
}
