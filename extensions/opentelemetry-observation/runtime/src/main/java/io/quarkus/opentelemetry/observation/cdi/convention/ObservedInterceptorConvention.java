package io.quarkus.opentelemetry.observation.cdi.convention;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.quarkus.opentelemetry.observation.cdi.ObservedInterceptorContext;

public interface ObservedInterceptorConvention
        extends ObservationConvention<ObservedInterceptorContext> {

    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof ObservedInterceptorContext;
    }
}
