package io.quarkus.runtime;

/**
 * Event payload that is fired in a pre-shutdown phase if {@code quarkus.shutdown.delay-enabled=true}.
 * <p>
 * This event is observed as follows:
 *
 * <code><pre>
 *     void onPreShutdown(@Observes ShutdownDelayInitiatedEvent ev) {
 *         LOGGER.info("The application is about to shutdown...");
 *     }
 * </pre></code>
 *
 * The annotated method can access other injected beans.
 */
public class ShutdownDelayInitiatedEvent {
}
