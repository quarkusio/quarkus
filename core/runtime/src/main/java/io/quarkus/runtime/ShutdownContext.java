package io.quarkus.runtime;

import java.io.Closeable;
import java.io.IOException;

import org.jboss.logging.Logger;

/**
 * A context that can be passed into runtime recorders that allows for shutdown tasks to be added.
 *
 * Tasks are executed in the reverse order that they are added.
 */
public interface ShutdownContext {

    void addShutdownTask(Runnable runnable);

    /**
     * Register a shutdown task that is deferred for some unspecified amount of time
     * after the tasks registered via {@link #addShutdownTask(Runnable)}.
     * <p>
     * Despite the name, this method does <em>not</em> guarantee that the task
     * will run "last". The complexity of the service dependency graph means that
     * no such ordering guarantee is feasible: other services may stop after this
     * task runs, and the task may run before all other shutdown activity has
     * completed.
     * <p>
     * New code should use the {@code ActionBuilder} service model to express
     * shutdown ordering via explicit dependency edges, rather than relying on
     * this method's weak ordering semantics.
     *
     * @param runnable the shutdown task (must not be {@code null})
     * @deprecated Use service dependency edges to control shutdown ordering instead.
     */
    @Deprecated(forRemoval = true)
    void addLastShutdownTask(Runnable runnable);

    class CloseRunnable implements Runnable {

        static final Logger log = Logger.getLogger(ShutdownContext.class);

        private final Closeable closeable;

        public CloseRunnable(Closeable closeable) {
            this.closeable = closeable;
        }

        @Override
        public void run() {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException e) {
                log.error("Failed to close " + closeable, e);
            }
        }
    }
}
