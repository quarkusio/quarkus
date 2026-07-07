package io.quarkus.gradle.application.model;

/**
 * Determines how a development-mode process establishes a debugger
 * connection.
 */
public enum QuarkusApplicationDevDebugMode {
    /**
     * Listen for a debugger to connect to the development-mode JVM.
     */
    LISTEN,
    /**
     * Connect the development-mode JVM to a listening debugger.
     */
    CONNECT
}
