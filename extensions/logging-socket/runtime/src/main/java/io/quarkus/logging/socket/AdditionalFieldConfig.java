package io.quarkus.logging.socket;

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
}
