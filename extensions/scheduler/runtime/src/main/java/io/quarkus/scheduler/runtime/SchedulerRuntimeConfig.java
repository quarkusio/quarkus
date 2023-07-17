package io.quarkus.scheduler.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.scheduler.Scheduler;

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

    /**
     * Scheduler can be started in different modes. By default, the scheduler is not started unless a
     * {@link io.quarkus.scheduler.Scheduled} business method is found.
     */
    @ConfigItem
    public Optional<StartMode> startMode;

    public enum StartMode {

        /**
         * The scheduler is not started unless a {@link io.quarkus.scheduler.Scheduled} business method is found.
         */
        NORMAL,

        /**
         * The scheduler will be started even if no scheduled business methods are found.
         * <p>
         * This is necessary for "pure" programmatic scheduling.
         */
        FORCED,

        /**
         * Just like the {@link #FORCED} mode but the scheduler will not start triggering jobs until {@link Scheduler#resume()}
         * is called.
         * <p>
         * This can be useful to run some initialization logic that needs to be performed before the scheduler starts.
         */
        HALTED;
    }
}
