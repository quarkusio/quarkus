package io.quarkus.extest.runtime;

import java.util.List;

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
    /** A nested long primitive */
    @ConfigItem
    public long longPrimitive;
    /** A nested long list */
    @ConfigItem
    public List<Long> longList;

}
