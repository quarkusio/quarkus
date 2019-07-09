package io.quarkus.runtime;

/**
 * A context that can be passed into runtime recorders that allows for shutdown tasks to be added.
 *
 * Tasks are executed in the reverse order that they are added.
 */
public interface ShutdownContext {

    void addShutdownTask(Runnable runnable);
}
