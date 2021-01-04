package io.quarkus.quartz.runtime;

/**
 * Defines starting modes of quartz scheduler
 */
public enum QuartzStartMode {

    /**
     * Scheduler is not started unless a {@link io.quarkus.scheduler.Scheduled} business method is found.
     */
    NORMAL("normal"),

    /**
     * Scheduler will be started even if no scheduled business methods are found.
     * This is necessary for "pure" programmatic scheduling.
     */
    FORCED("forced"),

    /**
     * Just like forced mode but the scheduler will be not start triggering jobs until an explicit start is called
     * from the main scheduler.
     * This is useful to programmatically register listeners before scheduler starts performing some work.
     */
    HALTED("halted");

    private final String mode;

    /**
     * Constructor
     * 
     * @param mode mode simple name
     */
    QuartzStartMode(final String mode) {

        this.mode = mode;
    }

    /**
     * Checks if quartz start mode is {@link #FORCED}
     * 
     * @param mode configuration start mode
     * 
     * @return {@code true} whether user configuration is to start scheduler in forced mode, {@code false} otherwise
     */
    public static boolean isForced(final String mode) {
        return mode.equals(FORCED.mode);
    }

    /**
     * Checks if quartz start mode is {@link #HALTED}
     * 
     * @param mode configuration start mode
     * 
     * @return {@code true} whether user configuration is to start scheduler in halted mode, {@code false} otherwise
     */
    public static boolean isHalted(final String mode) {
        return mode.equals(HALTED.mode);
    }
}
