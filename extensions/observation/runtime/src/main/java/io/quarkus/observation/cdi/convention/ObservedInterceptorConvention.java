package io.quarkus.observation.cdi.convention;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.annotation.Observed;
import io.quarkus.observation.cdi.ObservedInterceptor;
import io.quarkus.observation.cdi.ObservedInterceptorContext;

/**
 * Provides the naming and KeyValues used by the {@link Observed} interceptor: {@link ObservedInterceptor}.
 * <p>
 * The default implementation is {@link DefaultObservedInterceptorConvention} and it
 * can be overridden by a user's CDI bean implemented this interface.
 */
public interface ObservedInterceptorConvention
        extends ObservationConvention<ObservedInterceptorContext> {

    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof ObservedInterceptorContext;
    }
}
