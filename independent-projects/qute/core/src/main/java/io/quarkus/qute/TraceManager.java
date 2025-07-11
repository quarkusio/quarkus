package io.quarkus.qute;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkus.qute.trace.ResolveEvent;
import io.quarkus.qute.trace.TemplateEvent;
import io.quarkus.qute.trace.TraceListener;

/**
 * Manager that holds and dispatches events to registered
 * {@link TraceListener}s.
 * <p>
 * Each {@link Engine} instance has its own {@code TraceManager} to coordinate
 * tracing callbacks during template rendering.
 */
public class TraceManager {

    private final List<TraceListener> listeners;

    public TraceManager() {
        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Registers a new trace listener.
     *
     * @param listener the listener to add
     */
    public void addTraceListener(TraceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered trace listener.
     *
     * @param listener the listener to remove
     */
    public void removeTraceListener(TraceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fires an event to all listeners indicating the start of a template rendering.
     *
     * @param event the template event
     */
    void fireStartTemplate(TemplateEvent event) {
        if (hasTraceListeners()) {
            for (TraceListener listener : listeners) {
                listener.onStartTemplate(event);
            }
        }
    }

    /**
     * Fires an event to all listeners before a template node is resolved.
     *
     * @param event the resolve event
     */
    void fireBeforeResolveEvent(ResolveEvent event) {
        if (hasTraceListeners()) {
            for (TraceListener listener : listeners) {
                listener.onBeforeResolve(event);
            }
        }
    }

    /**
     * Fires an event to all listeners after a template node has been resolved.
     *
     * @param event the resolve event
     */
    void fireAfterResolveEvent(ResolveEvent event) {
        if (hasTraceListeners()) {
            for (TraceListener listener : listeners) {
                listener.onAfterResolve(event);
            }
        }
    }

    /**
     * Fires an event to all listeners indicating the end of a template rendering.
     *
     * @param event the template event
     */
    void fireEndTemplate(TemplateEvent event) {
        if (hasTraceListeners()) {
            for (TraceListener listener : listeners) {
                listener.onEndTemplate(event);
            }
        }
    }

    /**
     * Returns {@code true} if there is at least one registered listener.
     *
     * @return whether any trace listeners are registered
     */
    public boolean hasTraceListeners() {
        return !listeners.isEmpty();
    }
}