package io.quarkus.scheduler;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

/**
 * The container provides a built-in bean with bean type {@link Scheduler}, scope {@link ApplicationScoped}, and qualifier
 * {@link Default}.
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
     * Start a one-shot timer to fire after the specified delay.
     *
     * @param delay The delay in milliseconds,
     * @param action The action to run
     */
    void startTimer(long delay, Runnable action);

}
