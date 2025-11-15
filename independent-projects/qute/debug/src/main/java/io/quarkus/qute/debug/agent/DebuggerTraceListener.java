package io.quarkus.qute.debug.agent;

import io.quarkus.qute.trace.ResolveEvent;
import io.quarkus.qute.trace.TemplateEvent;
import io.quarkus.qute.trace.TraceListener;

/**
 * A {@link TraceListener} implementation that connects the Qute engine's trace
 * events to the {@link DebuggeeAgent}.
 * <p>
 * This listener is responsible for intercepting template execution events and
 * forwarding them to the debugger agent so that they can be processed for
 * debugging purposes (breakpoints, stepping, etc.).
 * </p>
 */
public class DebuggerTraceListener implements TraceListener {

    private final DebuggeeAgent agent;

    /**
     * Creates a new trace listener for the given debugger agent.
     *
     * @param agent the {@link DebuggeeAgent} that will receive the traced events,
     *        must not be {@code null}
     */
    public DebuggerTraceListener(DebuggeeAgent agent) {
        this.agent = agent;
    }

    /**
     * Called before a template node is resolved.
     * <p>
     * This method is invoked for every template node about to be processed, and
     * it forwards the event to the {@link DebuggeeAgent} so that it can handle
     * breakpoints and stepping at a fine-grained level.
     * </p>
     *
     * @param event the resolve event triggered by the Qute engine
     */
    @Override
    public void onBeforeResolve(ResolveEvent event) {
        agent.onBeforeResolve(event);
    }

    @Override
    public void onAfterResolve(ResolveEvent event) {
        agent.onAfterResolve(event);
    }

    /**
     * Called when the rendering of a template starts.
     * <p>
     * This event signals the beginning of template execution and allows the
     * debugger to create and track a new debuggee thread.
     * </p>
     *
     * @param event the template start event
     */
    @Override
    public void onStartTemplate(TemplateEvent event) {
        agent.onStartTemplate(event);
    }

    /**
     * Called when the rendering of a template ends.
     * <p>
     * This event signals that the template execution has completed, allowing
     * the debugger to clean up resources associated with the thread.
     * </p>
     *
     * @param event the template end event
     */
    @Override
    public void onEndTemplate(TemplateEvent event) {
        agent.onEndTemplate(event);
    }

}
