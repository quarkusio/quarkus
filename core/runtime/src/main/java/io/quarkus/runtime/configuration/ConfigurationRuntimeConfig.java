package io.quarkus.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "configuration", phase = ConfigPhase.RUN_TIME)
public class ConfigurationRuntimeConfig {

    /**
     * What should happen if the application is started with a different build time configuration than it was compiled
     * against. This may be useful to prevent misconfiguration.
     * <p>
     * If this is set to {@code warn} the application will warn at start up.
     * <p>
     * If this is set to {@code fail} the application will fail at start up.
     * <p>
     * Native tests leveraging<code>@io.quarkus.test.junit.TestProfile</code> are always run with
     * {@code quarkus.configuration.build-time-mismatch-at-runtime = fail}.
     */
    @ConfigItem(defaultValue = "warn")
    public BuildTimeMismatchAtRuntime buildTimeMismatchAtRuntime;

    public enum BuildTimeMismatchAtRuntime {
        warn,
        fail
    }

}
