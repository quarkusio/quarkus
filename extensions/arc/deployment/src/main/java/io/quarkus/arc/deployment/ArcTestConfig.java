package io.quarkus.arc.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ArcTestConfig {

    /**
     * If set to true then disable {@code StartupEvent} and {@code ShutdownEvent} observers declared on application bean classes
     * during the tests.
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableApplicationLifecycleObservers;

}
