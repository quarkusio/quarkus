package org.jboss.shamrock.extest;

import java.util.OptionalLong;

import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;

@ConfigGroup()
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
}
