package io.quarkus.logging.json.runtime;

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
     * Supported types: {@code string}, {@code int}, and {@code long}.
     * String is the default if not specified.
     */
    @ConfigItem(defaultValue = "string")
    public Type type;

    public enum Type {
        STRING,
        INT,
        LONG,
    }
}
