package io.quarkus.observation;

import java.util.List;

import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.observation.opentelemetry.handler.OpenTelemetryObservationHandler;
import io.quarkus.observation.opentelemetry.handler.PropagatingReceiverTracingObservationHandler;
import io.quarkus.observation.opentelemetry.handler.PropagatingSenderTracingObservationHandler;
import io.quarkus.observation.opentelemetry.handler.TracingAwareMeterObservationHandler;
import io.quarkus.observation.propagation.QuarkusObservationRegistry;

public class ObservationRegistryCreator implements BeanCreator<ObservationRegistry> {

    public static final String HAS_OTEL_TRACING = "hasOtelTracing";

    @SuppressWarnings("rawtypes")
    @Override
    public ObservationRegistry create(SyntheticCreationalContext<ObservationRegistry> context) {
        ObservationRegistry registry = new QuarkusObservationRegistry();

        if ((boolean) context.getParams().get(HAS_OTEL_TRACING)) {
            List<ObservationHandler<? extends Observation.Context>> tracingHandlers = List.of(
                    context.getInjectedReference(PropagatingReceiverTracingObservationHandler.class),
                    context.getInjectedReference(PropagatingSenderTracingObservationHandler.class),
                    context.getInjectedReference(OpenTelemetryObservationHandler.class));
            registry.observationConfig().observationHandler(
                    new FirstMatchingCompositeObservationHandler(tracingHandlers));
        }

        registry.observationConfig().observationHandler(
                context.getInjectedReference(TracingAwareMeterObservationHandler.class));

        ArcContainer container = Arc.container();
        for (InstanceHandle<ObservationFilter> handle : container.listAll(ObservationFilter.class)) {
            registry.observationConfig().observationFilter(handle.get());
        }
        for (InstanceHandle<ObservationPredicate> handle : container.listAll(ObservationPredicate.class)) {
            registry.observationConfig().observationPredicate(handle.get());
        }
        for (InstanceHandle<GlobalObservationConvention> handle : container
                .listAll(GlobalObservationConvention.class)) {
            registry.observationConfig().observationConvention(handle.get());
        }

        return registry;
    }
}
