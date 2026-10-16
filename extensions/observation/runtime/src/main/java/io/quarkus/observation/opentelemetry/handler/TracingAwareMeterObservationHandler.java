package io.quarkus.observation.opentelemetry.handler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

@Singleton
@Priority(ObservationHandlerPriorities.METER_TRACING_AWARE)
public class TracingAwareMeterObservationHandler
        implements ObservationHandler<Observation.Context> {

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    Instance<MeterObservationHandler<?>> userMeterHandlers;

    private MeterObservationHandler<Observation.Context> delegate;

    @SuppressWarnings("unchecked")
    @PostConstruct
    void init() {
        if (userMeterHandlers.isResolvable()) {
            this.delegate = (MeterObservationHandler<Observation.Context>) userMeterHandlers.get();
        } else {
            this.delegate = new DefaultMeterObservationHandler(meterRegistry);
        }
    }

    @Override
    public void onStart(Observation.Context context) {
        delegate.onStart(context);
    }

    @Override
    public void onError(Observation.Context context) {
        delegate.onError(context);
    }

    @Override
    public void onEvent(Observation.Event event, Observation.Context context) {
        delegate.onEvent(event, context);
    }

    @Override
    public void onScopeOpened(Observation.Context context) {
        delegate.onScopeOpened(context);
    }

    @Override
    public void onScopeClosed(Observation.Context context) {
        delegate.onScopeClosed(context);
    }

    @Override
    public void onStop(Observation.Context context) {
        delegate.onStop(context);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return delegate.supportsContext(context);
    }
}
