package io.quarkus.runtime;

/**
 * Event that is fired before shutdown and can be inspected for shutdown cause.
 * See {@link ShutdownEvent#isStandardShutdown()}
 *
 * This event is observed as follows:
 *
 * <code><pre>
 *     void onStop(@Observes ShutdownEvent ev) {
 *         LOGGER.info("The application is stopping...");
 *     }
 * </pre></code>
 *
 * The annotated method can access other injected beans.
 */
public class ShutdownEvent extends jakarta.enterprise.event.Shutdown {

    private final ShutdownReason shutdownReason;

    public ShutdownEvent() {
        this.shutdownReason = ShutdownReason.STANDARD;
    }

    public ShutdownEvent(ShutdownReason shutdownReason) {
        this.shutdownReason = shutdownReason;
    }

    /**
     * Returns {@code true} if the application shutdown is considered standard; i.e. by exiting {@code main()} method or
     * executing either {@link Quarkus#asyncExit()} or {@link Quarkus#blockingExit()}.
     * <p>
     * All other cases are non-standard - {@code SIGINT}, {@code SIGTERM}, {@code System.exit(n} and so on.
     * Sending {@code CTRL + C} to running app in terminal is also non-standard shutdown.
     *
     * @return true if the app shutdown was standard, false otherwise
     */
    public boolean isStandardShutdown() {
        return shutdownReason.equals(ShutdownReason.STANDARD);
    }

    /**
     * An enum with values reflecting the reason for application shutdown.
     */
    enum ShutdownReason {
        /**
         * When {@code main()} method exits or when either {@link Quarkus#asyncExit()} or
         * {@link Quarkus#blockingExit()} was executed
         */
        STANDARD,
        /**
         * All other cases - {@code SIGINT}, {@code SIGTERM}, {@code System.exit(n} and so on.
         * This includes sending {@code CTRL + C} to running app in terminal.
         */
        NON_STANDARD;
    }
}
