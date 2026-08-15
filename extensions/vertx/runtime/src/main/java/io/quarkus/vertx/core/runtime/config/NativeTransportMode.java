package io.quarkus.vertx.core.runtime.config;

/**
 * Controls whether and how strictly native transport should be used.
 */
public enum NativeTransportMode {

    /**
     * Do not use native transport. Quarkus will always use Java NIO.
     */
    DISABLED,

    /**
     * Use native transport if available, fall back to Java NIO otherwise.
     */
    IF_AVAILABLE,

    /**
     * Require native transport. If the transport dependency is missing from the classpath,
     * the build fails. If the native library cannot be loaded at runtime, the application
     * fails to start.
     */
    REQUIRED
}
