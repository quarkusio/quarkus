package io.quarkus.extest.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This is a duplicate of {@linkplain TestBuildTimeConfig} with a {@linkplain ConfigPhase#BUILD_AND_RUN_TIME_FIXED}
 */
@ConfigRoot(name = "btrt", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class TestBuildAndRunTimeConfig {
    /** A config string */
    @ConfigItem
    public String btStringOpt;
    /** A config string with default value */
    @ConfigItem(defaultValue = "btStringOptWithDefaultValue")
    public String btStringOptWithDefault;
    /** A config object with ctor(String) */
    @ConfigItem
    public StringBasedValue btSBV;
    /** A config object with ctor(String) and default value */
    @ConfigItem(defaultValue = "btSBVWithDefaultValue")
    public StringBasedValue btSBVWithDefault;
    /** A nested config group with all supported value types */
    @ConfigItem
    public AllValuesConfig allValues;

    public TestBuildAndRunTimeConfig() {

    }
}
