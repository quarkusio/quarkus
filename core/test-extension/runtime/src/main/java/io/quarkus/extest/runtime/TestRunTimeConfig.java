package io.quarkus.extest.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rt", phase = ConfigPhase.RUN_TIME)
public class TestRunTimeConfig {
    /** A map of properties */
    @ConfigItem
    public Map<String, String> stringMap;
    /** A map of property lists */
    @ConfigItem
    public Map<String, List<String>> stringListMap;
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
                ", stringListMap=" + stringListMap +
                ", stringMap=" + stringMap +
                '}';
    }
}
