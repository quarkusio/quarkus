package io.quarkus.scheduler;

/**
 * The container provides a built-in bean with bean type {@link Scheduler} and qualifier
 * {@link javax.enterprise.inject.Default}.
 */
public interface Scheduler {

    /**
     * Pause the scheduler. No triggers are fired.
     */
    void pause();

    /**
     * Pause a specific job. Identity must not be null and non-existent identity results in no-op.
     *
     * @param identity
     * @see Scheduled#identity()
     */
    void pause(String identity);

    /**
     * Resume the scheduler. Triggers can be fired again.
     */
    void resume();

    /**
     * Resume a specific job. Identity must not be null and non-existent identity results in no-op.
     *
     * @param identity
     * @see Scheduled#identity()
     */
    void resume(String identity);

    /**
     * Identity must not be null and {@code false} is returned for non-existent identity.
     * 
     * @param identity
     * @return {@code true} if the job with the given identity is paused, {@code false} otherwise
     * @see Scheduled#identity()
     */
    boolean isPaused(String identity);

    /**
     * @return {@code true} if a scheduler is running the triggers are fired and jobs are executed, {@code false} otherwise
     */
    boolean isRunning();

}
