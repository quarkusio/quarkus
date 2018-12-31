package org.jboss.shamrock.test;

/**
 * Manage the lifecycle of a test resource, for instance a H2 test server.
 */
public interface ShamrockTestResourceLifecycleManager {

    /**
     * Start the test resource.
     */
    void start();

    /**
     * Stop the test resource.
     */
    void stop();
}
