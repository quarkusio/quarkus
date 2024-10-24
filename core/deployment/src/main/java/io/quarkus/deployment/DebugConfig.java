package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Debugging
 * <p>
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.debug.reflection=true
 *
 * TODO refactor code to actually use these values
 */
@ConfigMapping(prefix = "quarkus.debug")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DebugConfig {

    /**
     * If set to true, writes a list of all reflective classes to META-INF
     */
    @WithDefault("false")
    boolean reflection();

    /**
     * If set to a directory, all generated classes will be written into that directory
     */
    Optional<String> generatedClassesDir();

    /**
     * If set to a directory, all transformed classes (e.g. Panache entities) will be written into that directory
     */
    Optional<String> transformedClassesDir();

    /**
     * If set to a directory, ZIG files for generated code will be written into that directory.
     * <p>
     * A ZIG file is a textual representation of the generated code that is referenced in the stacktraces.
     */
    Optional<String> generatedSourcesDir();

    /**
     * If set to true then dump the build metrics to a JSON file in the build directory.
     */
    @WithDefault("false")
    boolean dumpBuildMetrics();
}
