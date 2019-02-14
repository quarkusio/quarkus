package org.jboss.shamrock.extest;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "bt", phase = ConfigPhase.BUILD_TIME)
public class TestBuildTimeConfig {
    /** A build time string */
    @ConfigItem()
    String btStringOpt;
    /** A build time string with default value */
    @ConfigItem(defaultValue = "btStringOptWithDefaultValue")
    String btStringOptWithDefault;
    /** A build time object with ctor(String) */
    @ConfigItem
    StringBasedValue btSBV;
    /** A build time object with ctor(String) and default value */
    @ConfigItem(defaultValue = "btSBVWithDefaultValue")
    StringBasedValue btSBVWithDefault;
    /** A config group with all supported value types */
    @ConfigItem
    AllValuesConfig allValues;

}
