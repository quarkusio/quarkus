package io.quarkus.quartz.runtime;

/**
 * Defines starting modes of quartz scheduler
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
     * Just like forced mode but the scheduler will be not start triggering jobs until an explicit start is called
     * from the main scheduler.
     * This is useful to programmatically register listeners before scheduler starts performing some work.
     */
    HALTED;

    /**
     * Checks if quartz start mode is {@link #FORCED}
     * 
     * @return {@code true} whether user configuration is to start scheduler in forced mode, {@code false} otherwise
     */
    public boolean isForced() {
        return this == FORCED;
    }

    /**
     * Checks if quartz start mode is {@link #HALTED}
     * 
     * @return {@code true} whether user configuration is to start scheduler in halted mode, {@code false} otherwise
     */
    public boolean isHalted() {
        return this == HALTED;
    }
}
