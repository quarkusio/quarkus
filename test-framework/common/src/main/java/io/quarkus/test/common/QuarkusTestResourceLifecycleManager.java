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
 * twice.
 *
 * Note that when using these with QuarkusUnitTest (rather than @QuarkusTest) they run
 * before the ClassLoader has been setup. This means injection may not work
 * as expected.
 * <p>
 * Due to the very early execution in the test setup lifecycle, logging does <b>not</b>
 * work in such a manager.
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
     * Arguments passed to the lifecycle manager before it starts
     * These arguments are taken from {@code QuarkusTestResource#initArgs()}
     *
     * The {@code args} is never null
     *
     * @see QuarkusTestResource#initArgs()
     */
    default void init(Map<String, String> initArgs) {

    }

    /**
     * Allow each resource to provide custom injection of fields of the test class
     */
    default void inject(Object testInstance) {
    }

    /**
     * If multiple Test Resources are specified,
     * this control the order of which they will be executed.
     *
     * @return The order to be executed. The larger the number, the later the Resource is invoked.
     */
    default int order() {
        return 0;
    }
}
