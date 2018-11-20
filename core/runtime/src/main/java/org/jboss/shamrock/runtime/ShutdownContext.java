package org.jboss.shamrock.runtime;

/**
 * A context that can be passed into runtime templates that allows for shutdown tasks to be added.
 *
 * Tasks are executed in the reverse order that they are added.
 */
public interface ShutdownContext {

    void addShutdownTask(Runnable runnable);
}
