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

    /**
     * Priority that define the order in which shutdown tasks are invoked.
     */
    interface Priority {
        int value();

        /**
         * For shutdown tasks that have to be executed before the {@link ShutdownEvent application shutdown event}.
         */
        static Priority application(int priority) {
            if (priority == 0) {
                // do not allow 0 priority as it has a "unique" meaning here.
                throw new IllegalArgumentException("Use the applicationShutdownEvent() instead.");
            }
            return PriorityImpl.priority(priority, PriorityImpl.APPLICATION_VALUE, Integer.MAX_VALUE);
        }

        /**
         * This "unique" priority represents the execution of the {@link ShutdownEvent application shutdown event}.
         */
        static Priority applicationShutdownEvent() {
            return PriorityImpl.APPLICATION;
        }

        /**
         * For extension shutdown tasks that are executed after all {@link ShutdownEvent application shutdown event} handlers,
         * but may still require access to some CDI beans.
         */
        static Priority extensionPreCdi(int priority) {
            return PriorityImpl.priority(priority, PriorityImpl.EXTENSIONS_VALUE, PriorityImpl.EXTENSIONS_CDI_VALUE);
        }

        /**
         * This "unique" priority represents the shutdown of the CDI container, and any shutdown tasks after it won't be able to
         * leverage any CDI beans.
         */
        static Priority cdiShutdown() {
            return PriorityImpl.EXTENSIONS_CDI;
        }

        /**
         * For extension shutdown tasks that do not require CDI beans.
         */
        static Priority extensionPostCdi(int priority) {
            return PriorityImpl.priority(priority, PriorityImpl.EXTENSIONS_VALUE, PriorityImpl.EXTENSIONS_CDI_VALUE);
        }

        /**
         * Same as {@link #extensionPostCdi(int)} with the default priority for convenience.
         */
        static Priority extensionPostCdi() {
            return PriorityImpl.EXTENSION;
        }

        /**
         * For core extensions like Vert.x that have to be shut down in the very end.
         */
        static Priority core(int priority) {
            return PriorityImpl.priority(priority, PriorityImpl.CORE_VALUE, PriorityImpl.EXTENSIONS_VALUE);
        }

        /**
         * Same as {@link #core(int)} with the default priority for convenience.
         */
        static Priority core() {
            return PriorityImpl.CORE;
        }

        /**
         * Most likely an unusable range of priorities to run shutdown tasks after core extensions were already shut down.
         */
        static Priority afterCore(int priority) {
            return PriorityImpl.priority(priority, 0, PriorityImpl.CORE_VALUE);
        }

        record PriorityImpl(int value) implements Priority {
            private static final int CORE_VALUE = 100_000;
            private static final int EXTENSIONS_VALUE = 500_000;
            private static final int EXTENSIONS_CDI_VALUE = 550_000;
            private static final int APPLICATION_VALUE = 1_000_000;

            private static final Priority CORE = new PriorityImpl(CORE_VALUE);
            private static final Priority EXTENSION = new PriorityImpl(EXTENSIONS_VALUE);
            private static final Priority EXTENSIONS_CDI = new PriorityImpl(EXTENSIONS_CDI_VALUE);
            private static final Priority APPLICATION = new PriorityImpl(APPLICATION_VALUE);

            private static Priority priority(int priority, int min, int max) {
                int value;
                if (priority > 0) {
                    value = min + priority;
                } else {
                    value = max + priority;
                }
                if (value >= max || value < min) {
                    throw new IllegalArgumentException("Priority value " + priority + " (effective: " + value
                            + ") is not within the allowed range for this group: [" + min + ", " + max + ")");
                }
                return new PriorityImpl(value);
            }
        }

    }

    default void addShutdownTask(Runnable runnable) {
        addShutdownTask(Priority.extensionPostCdi(), runnable);
    }

    /**
     * @param priority higher the priority -- sooner the shutdown task is going to be performed.
     * @param runnable the shutdown task to perform.
     */
    void addShutdownTask(Priority priority, Runnable runnable);

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
