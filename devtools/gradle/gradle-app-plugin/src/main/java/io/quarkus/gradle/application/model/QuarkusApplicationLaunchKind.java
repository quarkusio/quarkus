package io.quarkus.gradle.application.model;

/**
 * Long-lived launch lifecycles coordinated by the standalone plugin.
 */
public enum QuarkusApplicationLaunchKind {
    /**
     * Run a packaged application.
     */
    RUN,
    /**
     * Run local development mode with continuous Gradle compilation.
     */
    DEV,
    /**
     * Synchronize build output to a remote development-mode application.
     */
    REMOTE_DEV,
    /**
     * Run continuous tests without starting an application endpoint.
     */
    CONTINUOUS_TEST
}
