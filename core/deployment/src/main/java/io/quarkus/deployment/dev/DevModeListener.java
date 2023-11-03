package io.quarkus.deployment.dev;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;

/**
 * SPI that can be used by extensions that need to run code in various phases of dev mode
 */
public interface DevModeListener {

    int DEFAULT_ORDER = 0;

    /**
     * Executed after the first successfull application start.
     *
     * @param application
     */
    void afterFirstStart(RunningQuarkusApplication application);

    void beforeShutdown();

    /**
     * Determines the order with which the listeners are executed. Classes with a lower order are executed
     * first during start (the order is reverse for shutdown)
     */
    default int order() {
        return DEFAULT_ORDER;
    }
}
