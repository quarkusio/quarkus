package io.quarkus.runtime.init;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = ConfigItem.PARENT, phase = ConfigPhase.RUN_TIME)
public class InitRuntimeConfig {

    /**
     * true to quit exit right after the initialization.
     * The option is not meant be used directly by users.
     *
     */
    @ConfigItem
    public boolean initAndExit;

}
