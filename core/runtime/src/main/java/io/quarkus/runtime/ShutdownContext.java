package io.quarkus.runtime;

import java.io.Closeable;
import java.io.IOException;

import jakarta.interceptor.Interceptor;

import org.jboss.logging.Logger;

/**
 * A context that can be passed into runtime recorders that allows for shutdown tasks to be added.
 *
 * Tasks are executed in the reverse order that they are added.
 */
public interface ShutdownContext {

    int DEFAULT_PRIORITY = Interceptor.Priority.LIBRARY_AFTER;
    int SHUTDOWN_EVENT_PRIORITY = DEFAULT_PRIORITY + 100_000;

    default void addShutdownTask(Runnable runnable) {
        addShutdownTask(DEFAULT_PRIORITY, runnable);
    }

    void addShutdownTask(int priority, Runnable runnable);

    // these are executed after all the ones added via addShutdownTask in the reverse order from which they were added
    default void addLastShutdownTask(Runnable runnable) {
        addLastShutdownTask(DEFAULT_PRIORITY, runnable);
    }

    void addLastShutdownTask(int priority, Runnable runnable);

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
