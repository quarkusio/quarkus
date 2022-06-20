package io.quarkus.qute;

import io.quarkus.qute.trace.ResolveEvent;
import io.quarkus.qute.trace.TemplateEvent;
import io.quarkus.qute.trace.TraceListener;
import java.util.ArrayList;
import java.util.List;

public class TraceManager {

    private List<TraceListener> listeners;

    public TraceManager() {
    }

    /**
     * Add a trace listener for the purposes of debugging and diagnosis.
     *
     * @param listener Trace listener to be added.
     *
     */
    public void addTraceListener(TraceListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }

    /**
     * Remove a trace listener.
     *
     * @param listener Trace listener to be removed.
     */
    public void removeTraceListener(TraceListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                listeners = null;
            }
        }
    }

    void fireStartTemplate(TemplateEvent event) {
        if (hasTraceListeners()) {
            for (TraceListener listener : listeners) {
                listener.startTemplate(event);
            }
        }
    }

    void fireBeforeResolveEvent(ResolveEvent event) {
        if (hasTraceListeners()) {
            for (TraceListener listener : listeners) {
                listener.beforeResolve(event);
            }
        }
    }

    void fireAfterResolveEvent(ResolveEvent event) {
        if (hasTraceListeners()) {
            for (TraceListener listener : listeners) {
                listener.afterResolve(event);
            }
        }
    }

    void fireEndTemplate(TemplateEvent event) {
        if (hasTraceListeners()) {
            for (TraceListener listener : listeners) {
                listener.endTemplate(event);
            }
        }
    }

    private boolean hasTraceListeners() {
        return listeners != null;
    }
}
