package io.quarkus.opentelemetry.observation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.observation.handler.OpenTelemetryObservationHandler;
import io.quarkus.opentelemetry.observation.handler.PropagatingReceiverTracingObservationHandler;
import io.quarkus.opentelemetry.observation.handler.PropagatingSenderTracingObservationHandler;
import io.quarkus.opentelemetry.observation.handler.TracingAwareMeterObservationHandler;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ObservationOpenTelemetryRecorder {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Function<SyntheticCreationalContext<ObservationRegistry>, ObservationRegistry> createObservationRegistry() {
        return new Function<>() {
            @Override
            public ObservationRegistry apply(SyntheticCreationalContext<ObservationRegistry> context) {
                ArcContainer container = Arc.container();

                ObservationRegistry registry = ObservationRegistry.create();

                // Tracing handlers — ordered by priority (highest first = most specific first)
                List<ObservationHandler<? extends Observation.Context>> tracingHandlers = new ArrayList<>();
                addIfAvailable(container, PropagatingReceiverTracingObservationHandler.class, tracingHandlers);
                addIfAvailable(container, PropagatingSenderTracingObservationHandler.class, tracingHandlers);
                addIfAvailable(container, OpenTelemetryObservationHandler.class, tracingHandlers);

                if (!tracingHandlers.isEmpty()) {
                    registry.observationConfig().observationHandler(
                            new FirstMatchingCompositeObservationHandler(tracingHandlers));
                }

                // Meter handler — runs independently
                InstanceHandle<TracingAwareMeterObservationHandler> meterHandle = container
                        .instance(TracingAwareMeterObservationHandler.class);
                if (meterHandle.isAvailable()) {
                    registry.observationConfig().observationHandler(meterHandle.get());
                }

                // User-provided extensions
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
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void addIfAvailable(ArcContainer container, Class<? extends ObservationHandler> handlerClass,
            List<ObservationHandler<? extends Observation.Context>> list) {
        InstanceHandle handle = container.instance(handlerClass);
        if (handle.isAvailable()) {
            list.add((ObservationHandler<? extends Observation.Context>) handle.get());
        }
    }
}
