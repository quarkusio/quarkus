package io.quarkus.runtime.init;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = ConfigItem.PARENT, phase = ConfigPhase.RUN_TIME)
public class InitRuntimeConfig {

    /**
     * Filter initialization tasks.
     * Support exact match or expressions
     */
    @ConfigItem(defaultValue = "*")
    public String initTaskFilter;

    /**
     * Feature flag to disable initialization
     */
    @ConfigItem
    public boolean initDisabled;

    /**
     * true to quit exit right after the initialization.
     * The option is not meant be used directly by users.
     */
    @ConfigItem
    public boolean initAndExit;
}
