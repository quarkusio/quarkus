package io.quarkus.scheduler.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.scheduler.Scheduler;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.scheduler")
public interface SchedulerRuntimeConfig {

    /**
     * If schedulers are enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Scheduled task will be flagged as overdue if next execution time is exceeded by this period.
     */
    @WithDefault("1")
    Duration overdueGracePeriod();

    /**
     * Scheduler can be started in different modes. By default, the scheduler is not started unless a
     * {@link io.quarkus.scheduler.Scheduled} business method is found.
     */
    @WithDefault("normal")
    StartMode startMode();

    enum StartMode {

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
