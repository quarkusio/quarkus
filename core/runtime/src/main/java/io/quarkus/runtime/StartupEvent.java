package io.quarkus.runtime;

/**
 * Event class that is fired on startup.
 *
 * This is fired on main method execution after all startup code has run,
 * so can be used to start threads etc in native image mode
 *
 * This event is observed as follows:
 *
 * <code><pre>
 *     void onStart(@Observes StartupEvent ev) {
 *         LOGGER.info("The application is starting...");
 *     }
 * </pre></code>
 *
 * The annotated method can access other injected beans.
 *
 */
public class StartupEvent {
}
