package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
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
}
