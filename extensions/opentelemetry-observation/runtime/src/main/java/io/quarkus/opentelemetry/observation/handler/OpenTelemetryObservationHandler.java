package io.quarkus.opentelemetry.observation.handler;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import io.micrometer.observation.Observation;

@Singleton
@Priority(ObservationHandlerPriorities.DEFAULT_TRACING)
public class OpenTelemetryObservationHandler
        extends AbstractTracingObservationHandler<Observation.Context> {

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }
}