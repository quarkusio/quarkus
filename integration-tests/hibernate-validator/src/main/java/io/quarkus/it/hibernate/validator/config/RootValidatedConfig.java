package io.quarkus.it.hibernate.validator.config;

import jakarta.validation.constraints.Size;

import io.smallrye.config.ConfigMapping;

/**
 * A config mapping with a Bean Validation constraint directly on a root-level property.
 */
@ConfigMapping(prefix = "config-validation.root-validated")
public interface RootValidatedConfig {

    @Size(min = 2, max = 20)
    String value();
}
