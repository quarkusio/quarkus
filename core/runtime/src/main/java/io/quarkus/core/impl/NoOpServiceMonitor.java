package io.quarkus.core.impl;

/**
 * A service monitor that does nothing.
 */
public final class NoOpServiceMonitor extends ServiceMonitor {

    /**
     * The singleton instance of the no-op service monitor.
     */
    public static final NoOpServiceMonitor INSTANCE = new NoOpServiceMonitor();

    /**
     * Private constructor.
     */
    private NoOpServiceMonitor() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startInitiated(String name) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startComplete(String name) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startFailed(String name, String reason) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopInitiated(String name) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopComplete(String name) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopFailed(String name, String reason) {
    }
}
