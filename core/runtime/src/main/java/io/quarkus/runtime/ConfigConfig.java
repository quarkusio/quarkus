package io.quarkus.runtime;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocPrefix;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration.
 * <p>
 * We don't really use this, because these are configurations for the config itself, so it causes a chicken / egg
 * problem, but we need it for documentation purposes.
 * <br>
 * Relocation of the Config configurations to the Quarkus namespace is done in
 * {@link io.quarkus.runtime.configuration.ConfigUtils#configBuilder}.
 */
@ConfigMapping(prefix = "quarkus")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigDocPrefix("quarkus.config")
public interface ConfigConfig {
    /**
     * A comma separated list of profiles that will be active when Quarkus launches.
     */
    Optional<List<String>> profile();

    /**
     * Accepts a single configuration profile name. If a configuration property cannot be found in the current active
     * profile, the config performs the same lookup in the profile set by this configuration.
     */
    @WithName("config.profile.parent")
    Optional<String> profileParent();

    /**
     * Additional config locations to be loaded with the Config. The configuration support multiple locations
     * separated by a comma and each must represent a valid {@link java.net.URI}.
     */
    @WithName("config.locations")
    Optional<List<URI>> locations();

    /**
     * Validates that a <code>@ConfigMapping</code> maps every available configuration name contained in the mapping
     * prefix.
     */
    @WithName("config.mapping.validate-unknown")
    Optional<Boolean> mappingValidateUnknown();

    /**
     * Enable logging of configuration values lookup in DEBUG log level.
     * <br>
     * The log of configuration values require the category set to <code>DEBUG</code> in the
     * <code>io.smallrye.config</code> category: <code>quarkus.log.category."io.smallrye.config".level=DEBUG</code>.
     */
    @WithName("config.log.values")
    Optional<Boolean> logValues();

    /**
     * What should happen if the application is started with a different build time configuration than it was compiled
     * against. This may be useful to prevent misconfiguration.
     * <p>
     * If this is set to {@code warn} the application will warn at start up.
     * <p>
     * If this is set to {@code fail} the application will fail at start up.
     * <p>
     * Native tests leveraging<code>@io.quarkus.test.junit.TestProfile</code> are always run with
     * {@code quarkus.config.build-time-mismatch-at-runtime = fail}.
     */
    @WithName("config.build-time-mismatch-at-runtime")
    @WithDefault("warn")
    BuildTimeMismatchAtRuntime buildTimeMismatchAtRuntime();

    /**
     * A property that allows accessing a generated UUID.
     * It generates that UUID at startup time. So it changes between two starts including in dev mode.
     * <br>
     * Access this generated UUID using expressions: `${quarkus.uuid}`.
     */
    Optional<String> uuid();

    enum BuildTimeMismatchAtRuntime {
        warn,
        fail
    }
}
