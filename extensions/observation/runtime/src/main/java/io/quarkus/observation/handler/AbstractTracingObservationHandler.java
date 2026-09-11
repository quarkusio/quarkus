package io.quarkus.observation.handler;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

public abstract class AbstractTracingObservationHandler<T extends Observation.Context>
        implements ObservationHandler<T> {

    protected abstract void startSpan(T context);

    protected abstract void openScope(T context);

    protected abstract void closeScope(T context);

    protected abstract void recordError(T context, Throwable error);

    protected abstract void recordEvent(Observation.Event event, T context);

    protected abstract void stopSpan(T context);

    @Override
    public void onStart(T context) {
        startSpan(context);
    }

    @Override
    public void onScopeOpened(T context) {
        openScope(context);
    }

    @Override
    public void onScopeClosed(T context) {
        closeScope(context);
    }

    @Override
    public void onError(T context) {
        recordError(context, context.getError());
    }

    @Override
    public void onEvent(Observation.Event event, T context) {
        recordEvent(event, context);
    }

    @Override
    public void onStop(T context) {
        stopSpan(context);
    }
}
