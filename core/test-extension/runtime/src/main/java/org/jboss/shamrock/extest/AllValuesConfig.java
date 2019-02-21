package org.jboss.shamrock.extest;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;

@ConfigGroup
public class AllValuesConfig {
    /** a long primitive */
    @ConfigItem
    public long longPrimitive;
    /** a long value */
    @ConfigItem
    public Long longValue;
    /** an optional long value */
    @ConfigItem
    public OptionalLong optLongValue;
    /** an optional long value */
    @ConfigItem
    public Optional<Long> optionalLongValue;
    /** A config object with a static of(String) method */
    @ConfigItem
    public ObjectOfValue oov;
    /** A config object with a static of(String) method and default value */
    @ConfigItem(defaultValue = "defaultPart1+defaultPart2")
    public ObjectOfValue oovWithDefault;
    /** A config object with a static valueOf(String) method */
    @ConfigItem
    public ObjectValueOf ovo;
    /** A config object with a static of(String) method and default value */
    @ConfigItem(defaultValue = "defaultPart1+defaultPart2")
    public ObjectValueOf ovoWithDefault;
    /** */
    //@ConfigItem
    public Map<String, NestedConfig> nestedConfigMap;

    @Override
    public String toString() {
        return "AllValuesConfig{" +
                "longPrimitive=" + longPrimitive +
                ", longValue=" + longValue +
                ", optLongValue=" + optLongValue +
                ", optionalLongValue=" + optionalLongValue +
                ", oov=" + oov +
                ", oovWithDefault=" + oovWithDefault +
                ", ovo=" + ovo +
                ", ovoWithDefault=" + ovoWithDefault +
                '}';
    }
}
