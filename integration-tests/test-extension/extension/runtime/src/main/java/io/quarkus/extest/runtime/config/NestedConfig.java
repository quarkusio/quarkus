package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class NestedConfig {
    /** A nested string value */
    @ConfigItem
    public String nestedValue;
    /** A nested ObjectOfValue value */
    @ConfigItem
    public ObjectOfValue oov;

}
