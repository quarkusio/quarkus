package io.quarkus.scheduler;

/**
 * The container provides a built-in bean with bean type {@link Scheduler} and qualifier
 * {@link javax.enterprise.inject.Default}.
 *
 * @author Martin Kouba
 */
public interface Scheduler {

    /**
     * Pause all jobs.
     */
    void pause();

    /**
     * Resume all jobs.
     */
    void resume();

    /**
     * @return if a scheduler is running the triggers are fired and jobs are executed.
     */
    boolean isRunning();

}
