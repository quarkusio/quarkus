package io.quarkus.jlink.deployment;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for producing a {@code jlink} image.
 */
@ConfigRoot
@ConfigMapping(prefix = "quarkus.jlink")
// @WithDynamicDefaults(JLinkConfigDefaults.class)
public interface JLinkConfig {
    /**
     * {@return {@code true} to enable {@code jlink} execution, or {@code false} otherwise}
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * {@return the minimum heap size to configure for the image}
     * If not given, then no default value will be set in the resultant {@code jlink} image.
     */
    Optional<MemorySize> minHeapSize();

    /**
     * {@return the maximum heap size to configure for the image}
     * If not given, then no default value will be set in the resultant {@code jlink} image.
     */
    Optional<MemorySize> maxHeapSize();

    /**
     * {@return the image output path}
     * If relative, it will be resolved in terms of the packaging output path.
     */
    @WithDefault("image")
    Path imagePath();

    /**
     * {@return the launcher name}
     */
    //TODO TEMPORARY DEFAULT
    @WithDefault("my-app")
    String launcherName();

    /**
     * {@return the output directory}
     */
    // TODO: this should be based on some build system configuration
    @WithDefault("target/jlink-output")
    Path outputDirectory();

    /**
     * {@return the path of the staging directory for {@code jlink} output}
     */
    @WithDefault("staging")
    Path stagingDirectory();
}
