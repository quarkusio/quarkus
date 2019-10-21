package io.quarkus.deployment.pkg;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class PackageConfig {

    public static final String THIN_JAR = "thin-jar";
    public static final String UBER_JAR = "uber-jar";
    public static final String NATIVE = "native";

    /**
     * A list of requested output types. Even if some types are not explicitly requested as output
     * they may still be built if they are needed.
     * 
     * The default build in types are thin-jar, uber-jar and native
     */
    @ConfigItem(defaultValue = THIN_JAR)
    public List<String> types;

    /**
     * The entry point of the application. In most cases this should not be modified.
     */
    @ConfigItem(defaultValue = "io.quarkus.runner.GeneratedMain")
    public String mainClass;

    /**
     * Files that should not be copied to the output artifact
     */
    @ConfigItem
    public List<String> userConfiguredIgnoredEntries;

    /**
     * The suffix that is applied to the runner jar and native images
     */
    @ConfigItem(defaultValue = "-runner")
    public String runnerSuffix;
}
