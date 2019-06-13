package io.quarkus.runtime;

/**
 * Event that is fired before shutdown.
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
public class ShutdownEvent {
}
