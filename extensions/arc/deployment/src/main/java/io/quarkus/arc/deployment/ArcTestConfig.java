package io.quarkus.arc.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface ArcTestConfig {

    /**
     * If set to true then disable {@code StartupEvent} and {@code ShutdownEvent} observers declared on application bean classes
     * during the tests.
     */
    @WithDefault("false")
    boolean disableApplicationLifecycleObservers();

}
