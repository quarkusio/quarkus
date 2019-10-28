package io.quarkus.runtime;

/**
 * A context that can be passed into runtime recorders that allows for shutdown tasks to be added.
 *
 * Tasks are executed in the reverse order that they are added.
 */
public interface ShutdownContext {

    void addShutdownTask(Runnable runnable);

    // these are executed after all the ones add via addShutdownTask in the reverse order from which they were added
    void addLastShutdownTask(Runnable runnable);
}
