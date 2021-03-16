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
     * Resume the scheduler. Triggers can be fired again.
     */
    void resume();

    /**
     * @return if a scheduler is running the triggers are fired and jobs are executed.
     */
    boolean isRunning();

}
