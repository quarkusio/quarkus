package io.quarkus.scheduler.runtime;

import java.time.Duration;

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

    /**
     * Scheduled task will be flagged as overdue if next execution time is exceeded by this period.
     */
    @ConfigItem(defaultValue = "1")
    public Duration overdueGracePeriod;
}
