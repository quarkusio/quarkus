package io.quarkus.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Live reload.
 */
@ConfigMapping(prefix = "quarkus.live-reload")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface LiveReloadConfig {

    /**
     * Whether the live-reload feature should be enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether Quarkus should enable its ability to not do a full restart
     * when changes to classes are compatible with JVM instrumentation.
     * <p>
     * If this is set to true, Quarkus will perform class redefinition when possible.
     */
    @WithDefault("false")
    boolean instrumentation();

    /**
     * The names of additional resource files to watch for changes, triggering a reload on change. Directories are <em>not</em>
     * supported.
     */
    Optional<List<String>> watchedResources();

    /**
     * Password used to use to connect to the remote dev-mode application
     */
    Optional<String> password();

    /**
     * URL used to use to connect to the remote dev-mode application
     */
    Optional<String> url();

    /**
     * The amount of time to wait for a remote dev connect or reconnect
     */
    @WithDefault("30s")
    Duration connectTimeout();

    /**
     * The amount of time to wait between attempts when connecting to the server side of remote dev
     */
    @WithDefault("2s")
    Duration retryInterval();

    /**
     * The maximum number of attempts when connecting to the server side of remote dev
     */
    @WithDefault("10")
    Integer retryMaxAttempts();
}
