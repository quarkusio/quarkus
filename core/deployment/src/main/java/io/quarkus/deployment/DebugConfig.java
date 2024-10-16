package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Debugging
 * <p>
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.debug.reflection=true
 *
 * TODO refactor code to actually use these values
 */
@ConfigRoot
public class DebugConfig {

    /**
     * If set to true, writes a list of all reflective classes to META-INF
     */
    @ConfigItem(defaultValue = "false")
    boolean reflection;

    /**
     * If set to a directory, all generated classes will be written into that directory
     */
    @ConfigItem
    Optional<String> generatedClassesDir;

    /**
     * If set to a directory, all transformed classes (e.g. Panache entities) will be written into that directory
     */
    @ConfigItem
    Optional<String> transformedClassesDir;

    /**
     * If set to a directory, ZIG files for generated code will be written into that directory.
     * <p>
     * A ZIG file is a textual representation of the generated code that is referenced in the stacktraces.
     */
    @ConfigItem
    Optional<String> generatedSourcesDir;

    /**
     * If set to true then dump the build metrics to a JSON file in the build directory.
     */
    @ConfigItem(defaultValue = "false")
    boolean dumpBuildMetrics;
}
