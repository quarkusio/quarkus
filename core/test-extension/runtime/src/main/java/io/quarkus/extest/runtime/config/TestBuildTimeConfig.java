package io.quarkus.extest.runtime.config;

import java.util.List;
import java.util.Map;

import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * A root config object for use during build time
 */
@ConfigRoot(name = "bt", phase = ConfigPhase.BUILD_TIME)
public class TestBuildTimeConfig {
    /** A config string */
    @ConfigItem()
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

    public Map<String, Map<String, String>> mapMap;

    /**
     * Names of resources for which classpath entries should be recorded.
     * 
     * @see RecordedClasspathEntries#get(RecordedClasspathEntries.Phase, String)
     */
    public List<String> classpathEntriesToRecord;

    public TestBuildTimeConfig() {

    }
}
