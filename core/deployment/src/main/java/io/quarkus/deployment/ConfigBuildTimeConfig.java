package io.quarkus.deployment;

import io.quarkus.runtime.annotations.ConfigDocPrefix;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration
 */
@ConfigMapping(prefix = "quarkus.config")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigDocPrefix("quarkus.config")
public interface ConfigBuildTimeConfig {
    /**
     * <p>
     * Set this to <code>true</code> to read configuration from system properties and environment variables only. This
     * only applies to runtime.
     * </p>
     */
    @WithName("sources.system-only")
    @WithDefault("false")
    boolean systemOnly();

    /**
     * If set to {@code true}, all configuration is frozen at build time. All values from
     * {@code application.properties} and similar file-based sources are baked into the application artifact at the
     * highest priority. At runtime, no external configuration sources are consulted: no {@code application.properties},
     * no {@code .env} files, no system properties, and no environment variables can affect configuration values.
     * <p>
     * The only exceptions are a small set of JVM environment properties ({@code java.io.tmpdir},
     * {@code user.home}, {@code user.dir}) which are resolved at runtime to support
     * {@code @WithDefault} expressions whose values are inherently machine-dependent.
     * <p>
     * This is useful for CLI applications and sealed/immutable deployments where the binary's behavior must be
     * fully determined at build time.
     * <p>
     * Computed defaults ({@code @WithDefault}) and programmatic values set by recorders still work normally.
     * When enabled, this supersedes {@code quarkus.config.sources.system-only}.
     */
    @WithName("fixed-at-build-time")
    @WithDefault("false")
    boolean fixedAtBuildTime();
}
