package org.jboss.shamrock.extest;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rt", phase = ConfigPhase.RUN_TIME)
public class TestRunTimeConfig {
    /** A run time object */
    @ConfigItem
    String rtStringOpt;
    /** A run time object with default value */
    @ConfigItem(defaultValue = "rtStringOptWithDefaultValue")
    String rtStringOptWithDefault;
    /** A config group with all supported value types */
    @ConfigItem
    AllValuesConfig allValues;
}
