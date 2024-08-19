package io.quarkus.devservices.deployment.any;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface LabelConfig {

    /**
     * The value for the label
     */
    Optional<String> value();

}
