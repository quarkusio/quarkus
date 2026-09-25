package io.quarkus.uberjar.deployment;

import java.nio.file.Path;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for producing a modular UberJar.
 */
@ConfigRoot
@ConfigMapping(prefix = "quarkus.uberjar")
public interface UberJarConfig {
    /**
     * Whether modular UberJar generation is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The output filename of the modular UberJar, relative to the output directory.
     */
    @WithDefault("${quarkus.build.base-name:application}-modular-uberjar.jar")
    String filename();

    /**
     * The base output directory for the build.
     */
    @WithDefault("target/uberjar-output")
    Path outputDirectory();
}
