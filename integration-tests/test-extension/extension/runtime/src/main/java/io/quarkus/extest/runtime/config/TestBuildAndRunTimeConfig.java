package io.quarkus.extest.runtime.config;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;

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
    @ConfigDocSection
    public AllValuesConfig allValues;

    /**
     * Map of Integer conversion with {@link ConvertWith}
     */
    @ConfigItem
    @ConvertWith(WholeNumberConverter.class)
    public Map<String, Integer> mapOfNumbers;

    public Map<String, Map<String, String>> mapMap;

    /**
     * Enum object
     */
    @ConfigItem
    public MyEnum myEnum;
    /**
     * Enum list of objects
     */
    @ConfigItem
    public List<MyEnum> myEnums;

    public TestBuildAndRunTimeConfig() {

    }
}
