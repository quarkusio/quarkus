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
    long longPrimitive;
    /** a long value */
    @ConfigItem
    Long longValue;
    /** an optional long value */
    @ConfigItem
    OptionalLong optLongValue;
    /** an optional long value */
    @ConfigItem
    Optional<Long> optionalLongValue;
    /** A config object with a static of(String) method */
    @ConfigItem
    ObjectOfValue oov;
    /** A config object with a static of(String) method and default value */
    @ConfigItem(defaultValue = "defaultPart1+defaultPart2")
    ObjectOfValue oovWithDefault;
    /** A config object with a static valueOf(String) method */
    @ConfigItem
    ObjectValueOf ovo;
    /** A config object with a static of(String) method and default value */
    @ConfigItem(defaultValue = "defaultPart1+defaultPart2")
    ObjectValueOf ovoWithDefault;
    /** */
    //@ConfigItem
    Map<String, NestedConfig> nestedConfigMap;

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
