package io.quarkus.qute;

import io.quarkus.qute.trace.TraceListener;

/**
 * Manager that holds and dispatches events to registered
 * {@link TraceListener}s.
 * <p>
 * Each {@link Engine} instance has its own {@code TraceManager} to coordinate
 * tracing callbacks during template rendering.
 */
public interface TraceManager {

    /**
     * Registers a new {@link TraceListener} to receive trace events.
     * <p>
     * The listener will be notified of template rendering and resolution events.
     *
     * @param listener the trace listener to add; must not be {@code null}
     */
    void addTraceListener(TraceListener listener);

    /**
     * Unregisters a previously registered {@link TraceListener}.
     * <p>
     * After removal, the listener will no longer receive trace events.
     *
     * @param listener the trace listener to remove; must not be {@code null}
     */
    void removeTraceListener(TraceListener listener);

    /**
     * Returns {@code true} if there are any trace listeners currently registered,
     * {@code false} otherwise.
     * <p>
     * Trace listeners monitor and react to template rendering events.
     *
     * @return {@code true} if at least one trace listener is registered, {@code false} otherwise
     */
    boolean hasTraceListeners();

}
