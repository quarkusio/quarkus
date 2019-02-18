package org.jboss.shamrock.extest;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

/**
 * A root config object for use during build time
 */
@ConfigRoot(name = "bt", phase = ConfigPhase.BUILD_TIME)
public class TestBuildTimeConfig {
    /** A config string */
    @ConfigItem()
    String btStringOpt;
    /** A config string with default value */
    @ConfigItem(defaultValue = "btStringOptWithDefaultValue")
    String btStringOptWithDefault;
    /** A config object with ctor(String) */
    @ConfigItem
    StringBasedValue btSBV;
    /** A config object with ctor(String) and default value */
    @ConfigItem(defaultValue = "btSBVWithDefaultValue")
    StringBasedValue btSBVWithDefault;
    /** A nested config group with all supported value types */
    @ConfigItem
    AllValuesConfig allValues;

}
