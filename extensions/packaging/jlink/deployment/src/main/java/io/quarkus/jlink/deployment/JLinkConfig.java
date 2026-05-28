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
     * Whether {@code jlink} image generation is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The minimum heap size to configure for the image.
     * If not given, no minimum heap size is specified.
     */
    Optional<MemorySize> minHeapSize();

    /**
     * The maximum heap size to configure for the image.
     * If not given, no maximum heap size is specified.
     */
    Optional<MemorySize> maxHeapSize();

    /**
     * The image output path.
     * If relative, it will be resolved in terms of the packaging output path.
     */
    @WithDefault("image")
    Path imagePath();

    /**
     * The name of the launcher script generated in the image's {@code bin/} directory.
     */
    //TODO TEMPORARY DEFAULT
    @WithDefault("my-app")
    String launcherName();

    /**
     * The base output directory for the {@code jlink} build.
     */
    // TODO: this should be based on some build system configuration
    @WithDefault("target/jlink-output")
    Path outputDirectory();

    /**
     * The path of the staging directory used during the {@code jlink} build process, relative to the output directory.
     */
    @WithDefault("staging")
    Path stagingDirectory();
}
