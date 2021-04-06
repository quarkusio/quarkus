package io.quarkus.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LiveReloadConfig {

    /**
     * Whether or not Quarkus should enable its ability to not do a full restart
     * when changes to classes are compatible with JVM instrumentation.
     *
     * If this is set to true, Quarkus will perform class redefinition when possible.
     */
    @ConfigItem(defaultValue = "false")
    boolean instrumentation;

    /**
     * The names of additional resource files to watch for changes, triggering a reload on change. Directories are <em>not</em>
     * supported.
     */
    @ConfigItem
    public Optional<List<String>> watchedResources;

    /**
     * Password used to use to connect to the remote dev-mode application
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * URL used to use to connect to the remote dev-mode application
     */
    @ConfigItem
    public Optional<String> url;

    /**
     * The amount of time to wait for a remote dev connect or reconnect
     */
    @ConfigItem(defaultValue = "30s")
    public Duration connectTimeout;

    /**
     * The amount of time to wait between attempts when connecting to the server side of remote dev
     */
    @ConfigItem(defaultValue = "2s")
    public Duration retryInterval;

    /**
     * The maximum number of attempts when connecting to the server side of remote dev
     */
    @ConfigItem(defaultValue = "10")
    public Integer retryMaxAttempts;
}
