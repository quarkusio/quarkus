package io.quarkus.logging.gelf;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Post additional fields. E.g. `fieldName1=value1,fieldName2=value2`.
 */
@ConfigGroup
public class AdditionalFieldConfig {
    /**
     * Additional field value.
     */
    @ConfigItem
    public String value;

    /**
     * Additional field type specification.
     * Supported types: String, long, Long, double, Double and discover.
     * Discover is the default if not specified, it discovers field type based on parseability.
     */
    @ConfigItem(defaultValue = "discover")
    public String type;
}
