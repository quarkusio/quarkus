package io.quarkus.logging.gelf;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Post additional fields. E.g. `fieldName1=value1,fieldName2=value2`.
 */
@ConfigGroup
public interface AdditionalFieldConfig {
    /**
     * Additional field value.
     */
    String value();

    /**
     * Additional field type specification.
     * Supported types: String, long, Long, double, Double and discover.
     * Discover is the default if not specified, it discovers field type based on parseability.
     */
    @WithDefault("discover")
    String type();
}
