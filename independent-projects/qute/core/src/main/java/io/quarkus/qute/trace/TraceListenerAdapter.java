package io.quarkus.qute.trace;

/**
 * An empty implementation of the {@link TraceListener} interface.
 * <p>
 * This adapter class provides default (no-op) implementations of all methods in {@code TraceListener}.
 * It exists as a convenience for developers who want to override only a subset of the callback methods.
 * <p>
 * For example, to trace only template start and end events, you can subclass this adapter
 * and override {@link #onStartTemplate(TemplateEvent)} and {@link #onEndTemplate(TemplateEvent)}.
 *
 * @see TraceListener
 * @see TemplateEvent
 * @see ResolveEvent
 */
public class TraceListenerAdapter implements TraceListener {

    @Override
    public void onStartTemplate(TemplateEvent event) {

    }

    @Override
    public void onBeforeResolve(ResolveEvent event) {

    }

    @Override
    public void onAfterResolve(ResolveEvent event) {

    }

    @Override
    public void onEndTemplate(TemplateEvent event) {

    }

}
