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

    // these are executed after all the ones add via addShutdownTask in the reverse order from which they were added
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
