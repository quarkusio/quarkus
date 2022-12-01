package io.quarkus.quartz.runtime;

/**
 * Defines starting modes of Quartz scheduler
 */
public enum QuartzStartMode {

    /**
     * Scheduler is not started unless a {@link io.quarkus.scheduler.Scheduled} business method is found.
     */
    NORMAL,

    /**
     * Scheduler will be started even if no scheduled business methods are found.
     * This is necessary for "pure" programmatic scheduling.
     */
    FORCED,

    /**
     * Just like forced mode but the scheduler will not start triggering jobs until an explicit start is called
     * from the main scheduler.
     * This is useful to programmatically register listeners before scheduler starts performing some work.
     */
    HALTED;
}
