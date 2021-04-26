package io.quarkus.scheduler;

/**
 * The container provides a built-in bean with bean type {@link Scheduler} and qualifier
 * {@link javax.enterprise.inject.Default}.
 *
 * @author Martin Kouba
 */
public interface Scheduler {

    /**
     * Pause the scheduler. No triggers are fired.
     */
    void pause();

    /**
     * Pause a specific trigger. Identity must not be null and non-existent identity results in no-op.
     *
     * @param identity see {@link Scheduled#identity()}
     */
    void pause(String identity);

    /**
     * Resume the scheduler. Triggers can be fired again.
     */
    void resume();

    /**
     * Resume a specific trigger. Identity must not be null and non-existent identity results in no-op.
     *
     * @param identity see {@link Scheduled#identity()}
     */
    void resume(String identity);

    /**
     * @return if a scheduler is running the triggers are fired and jobs are executed.
     */
    boolean isRunning();

}
