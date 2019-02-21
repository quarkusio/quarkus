package org.jboss.shamrock.extest;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rt", phase = ConfigPhase.RUN_TIME)
public class TestRunTimeConfig {
    /** A run time object */
    @ConfigItem
    public String rtStringOpt;
    /** A run time object with default value */
    @ConfigItem(defaultValue = "rtStringOptWithDefaultValue")
    public String rtStringOptWithDefault;
    /** A config group with all supported value types */
    @ConfigItem
    public AllValuesConfig allValues;

    @Override
    public String toString() {
        return "TestRunTimeConfig{" +
                "rtStringOpt='" + rtStringOpt + '\'' +
                ", rtStringOptWithDefault='" + rtStringOptWithDefault + '\'' +
                ", allValues=" + allValues +
                '}';
    }
}
