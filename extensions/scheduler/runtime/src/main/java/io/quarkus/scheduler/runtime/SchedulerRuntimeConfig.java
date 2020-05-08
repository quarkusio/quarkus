package io.quarkus.scheduler.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class SchedulerRuntimeConfig {

    /**
     * If schedulers are enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

}