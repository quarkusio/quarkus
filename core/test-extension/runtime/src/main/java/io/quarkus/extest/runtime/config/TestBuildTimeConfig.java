package io.quarkus.extest.runtime.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries;
import io.quarkus.runtime.annotations.ConfigGroup;
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
     * Configuration related to recording of classpath entries.
     *
     * @see RecordedClasspathEntries#get(Path, RecordedClasspathEntries.Phase, String)
     */
    @ConfigItem
    public ClasspathRecordingConfig classpathRecording;

    @ConfigGroup
    public static class ClasspathRecordingConfig {

        /**
         * Names of resources for which classpath entries should be recorded.
         *
         * @see RecordedClasspathEntries#get(Path, RecordedClasspathEntries.Phase, String)
         */
        @ConfigItem
        public Optional<List<String>> resources;

        /**
         * Path to the file to which classpath entry records will be appended.
         *
         * @see RecordedClasspathEntries#get(Path, RecordedClasspathEntries.Phase, String)
         */
        @ConfigItem
        public Optional<Path> recordFile;

    }

    public TestBuildTimeConfig() {

    }
}
