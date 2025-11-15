package io.quarkus.qute;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkus.qute.trace.ResolveEvent;
import io.quarkus.qute.trace.TemplateEvent;
import io.quarkus.qute.trace.TraceListener;

/**
 *
 */
class TraceManagerImpl implements TraceManager {

    private final List<TraceListener> listeners;

    TraceManagerImpl() {
        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Registers a new trace listener.
     *
     * @param listener the listener to add
     */
    @Override
    public void addTraceListener(TraceListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    /**
     * Removes a previously registered trace listener.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeTraceListener(TraceListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
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
    @Override
    public boolean hasTraceListeners() {
        return !listeners.isEmpty();
    }
}
