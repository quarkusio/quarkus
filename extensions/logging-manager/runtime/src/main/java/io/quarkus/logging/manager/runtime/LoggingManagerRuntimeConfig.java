package io.quarkus.logging.manager.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "logging-manager", phase = ConfigPhase.RUN_TIME)
public class LoggingManagerRuntimeConfig {

    /**
     * If Logging Manager UI should be enabled. By default, Logging Manager UI is enabled if it is included (see
     * {@code always-include}).
     */
    @ConfigItem(name = "ui.enable", defaultValue = "true")
    boolean enable;
}
