package io.quarkus.core.impl;

/**
 * Monitors service startup and shutdown transitions.
 */
public abstract sealed class ServiceMonitor permits NoOpServiceMonitor, JfrServiceMonitor {

    /**
     * Package-private constructor to restrict subclassing.
     */
    ServiceMonitor() {
    }

    /**
     * Called when service startup has initiated.
     *
     * @param name the name of the service (must not be {@code null})
     */
    public abstract void startInitiated(String name);

    /**
     * Called when service startup has successfully completed.
     *
     * @param name the name of the service (must not be {@code null})
     */
    public abstract void startComplete(String name);

    /**
     * Called when service startup has failed.
     *
     * @param name the name of the service (must not be {@code null})
     * @param reason the string representation of the exception or failure reason (must not be {@code null})
     */
    public abstract void startFailed(String name, String reason);

    /**
     * Called when service stop has initiated.
     *
     * @param name the name of the service (must not be {@code null})
     */
    public abstract void stopInitiated(String name);

    /**
     * Called when service stop has successfully completed.
     *
     * @param name the name of the service (must not be {@code null})
     */
    public abstract void stopComplete(String name);

    /**
     * Called when service stop has failed.
     *
     * @param name the name of the service (must not be {@code null})
     * @param reason the string representation of the exception or failure reason (must not be {@code null})
     */
    public abstract void stopFailed(String name, String reason);
}
