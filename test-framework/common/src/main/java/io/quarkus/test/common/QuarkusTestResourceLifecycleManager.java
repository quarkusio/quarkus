package io.quarkus.test.common;

import java.util.Map;

/**
 * Manage the lifecycle of a test resource, for instance a H2 test server.
 * <p>
 * These resources are started before the first test is run, and are closed
 * at the end of the test suite. They are configured via the {@link QuarkusTestResource}
 * annotation, which can be placed on any class in the test suite.
 *
 * These can also be loaded via a service loader mechanism, however if a service
 * loader is used it should not also be annotated as this will result in it being executed
 * twice
 */
public interface QuarkusTestResourceLifecycleManager {

    /**
     * Start the test resource.
     *
     * @return A map of system properties that should be set for the running test
     */
    Map<String, String> start();

    /**
     * Stop the test resource.
     */
    void stop();

    /**
     * Allow each resource to provide custom injection of fields of the test class
     */
    default void inject(Object testInstance) {
    }
}
