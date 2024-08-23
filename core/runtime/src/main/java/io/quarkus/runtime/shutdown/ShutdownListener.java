package io.quarkus.runtime.shutdown;

/**
 * A listener that can be registered to control the shutdown process and implement
 * graceful shutdown.
 *
 * Shutdown happens in two phases. In the pre shutdown phase the application should
 * function normally, but should notify external systems that it is about to go away.
 *
 * In the shutdown phase the app should disallow new external requests, however
 * allow existing requests to complete normally.
 */
public interface ShutdownListener {

    /**
     * Pre shutdown notification, the listener can use this to notify external
     * systems this application is about to shut down.
     *
     * @param notification The notification event
     */
    default void preShutdown(ShutdownNotification notification) {
        notification.done();
    }

    /**
     * The shutdown notification. The listener should start rejecting requests
     * and wait for all existing ones to finish.
     *
     * @param notification The notification event
     */
    default void shutdown(ShutdownNotification notification) {
        notification.done();
    }

    interface ShutdownNotification {

        /**
         * This method must be invoked when the lister has done it's work,
         * to allow shutdown to proceed to the next stage.
         */
        void done();
    }

}
