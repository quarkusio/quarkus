package io.quarkus.qute;

import io.quarkus.qute.EngineBuilder.EngineListener;

/**
 * Utility class responsible for enabling Qute debugging support when running
 * Qute in standalone mode (i.e., outside a Quarkus application runtime).
 * <p>
 * This class checks configuration flags defined as **system properties** (using `-D`)
 * to determine whether the Qute debugger should be started.
 * Environment variables are still supported as a fallback, but using `-D` is preferred.
 * <p>
 * The debugger will be enabled if:
 * <ul>
 * <li>{@code -DquteDebugPort=<port>} is specified, or</li>
 * <li>{@code -DquteDebugEnabled=true} is specified</li>
 * </ul>
 * and only when Qute is not running inside a Quarkus runtime. If running inside Quarkus,
 * debugging is handled automatically via {@code DebugQuteEngineObserver}.
 *
 * <p>
 * To enable standalone debugging, the dependency {@code quarkus-qute-debug} must be present.
 * Otherwise, a message will be printed and debugging will be skipped.
 *
 * <h3>Supported system properties</h3>
 * <ul>
 * <li>{@code quteDebugPort} — Debug server port (optional)</li>
 * <li>{@code quteDebugEnabled} — Enables debugging even without a port (optional)</li>
 * <li>{@code quteDebugSuspend} — If true, pauses template rendering until a debugger attaches</li>
 * </ul>
 *
 * <p>
 * Example of launching a standalone Qute application with debugging enabled:
 *
 * <pre>
 * java -DquteDebugPort=5005 -DquteDebugSuspend=true -DquteDebugEnabled=true -jar my-qute-app.jar
 * </pre>
 */
public class DebuggerConfigurationUtils {

    private static final String QUTE_DEBUG_PORT = "quteDebugPort";
    private static final String QUTE_DEBUG_SUSPEND = "quteDebugSuspend";
    private static final String QUTE_DEBUG_ENABLED = "quteDebugEnabled";

    /**
     * Creates the debugger adapter listener if debugging is requested and Qute
     * is running in standalone mode.
     *
     * @return EngineListener for the debug server, or null if debugging is disabled or running inside Quarkus
     */
    public static EngineListener createDebuggerIfNeeded() {
        Integer port = getDebugPort();
        boolean enabled = isDebugEnabled();

        // Debug not explicitly enabled and no port defined → do nothing.
        if (port == null && !enabled) {
            return null;
        }

        // If we are inside Quarkus runtime, the debug is handled automatically by Quarkus.
        if (!isStandalone()) {
            return null;
        }

        // Try to load the standalone debug server implementation.
        try {
            Class<?> cls = Class.forName("io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter");
            // Use the no-argument constructor
            return (EngineListener) cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("You need to install the `quarkus-qute-debug` dependency to enable debugging.");
            return null;
        }
    }

    /**
     * Reads the debug port from system property or environment variable.
     *
     * @return the port number, or null if unset or invalid
     */
    public static Integer getDebugPort() {
        String port = getPropertyValue(QUTE_DEBUG_PORT);
        if (port == null || port.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns whether debugging is enabled.
     *
     * @return true if debugging is enabled via system property or environment variable
     */
    public static boolean isDebugEnabled() {
        return Boolean.parseBoolean(getPropertyValue(QUTE_DEBUG_ENABLED));
    }

    /**
     * Returns whether rendering should block until a debugger attaches.
     *
     * @return true if the suspend flag is set
     */
    public static boolean isDebugSuspend() {
        return Boolean.parseBoolean(getPropertyValue(QUTE_DEBUG_SUSPEND));
    }

    /**
     * Determines whether Qute is running in standalone mode (i.e., outside Quarkus runtime).
     *
     * @return true if standalone, false if running inside Quarkus
     */
    private static boolean isStandalone() {
        try {
            // If this class exists, we are inside Quarkus → not standalone
            Class.forName("io.quarkus.qute.runtime.debug.DebugQuteEngineObserver");
            return false;
        } catch (ClassNotFoundException e) {
            // Class missing → standalone environment
            return true;
        }
    }

    /**
     * Reads a configuration value from environment variable first, then system property.
     * <p>
     * For standalone applications, it is recommended to use system properties with `-D`.
     *
     * @param name the name of the property or environment variable
     * @return the property value, or null if unset
     */
    private static String getPropertyValue(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        return value;
    }
}
