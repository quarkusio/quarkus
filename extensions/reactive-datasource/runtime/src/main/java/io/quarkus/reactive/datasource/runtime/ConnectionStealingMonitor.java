package io.quarkus.reactive.datasource.runtime;

public interface ConnectionStealingMonitor {
    /**
     * Called whenever a reactive connection is acquired.
     *
     * @param datasourceName The name of the datasource (e.g., "default").
     * @param stolen 'true' if the connection is managed by a different event loop than the caller.
     */
    void connectionAcquired(String datasourceName, boolean stolen);
}