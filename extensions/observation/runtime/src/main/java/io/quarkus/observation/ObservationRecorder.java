package io.quarkus.observation;

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
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.observation.cdi.ObservedInterceptor;
import io.quarkus.observation.cdi.convention.ObservedInterceptorConvention;
import io.quarkus.observation.config.ObservationRuntimeConfig;
import io.quarkus.observation.handler.PrintOutHandler;
import io.quarkus.observation.opentelemetry.handler.OpenTelemetryObservationHandler;
import io.quarkus.observation.opentelemetry.handler.PropagatingReceiverTracingObservationHandler;
import io.quarkus.observation.opentelemetry.handler.PropagatingSenderTracingObservationHandler;
import io.quarkus.observation.opentelemetry.handler.TracingAwareMeterObservationHandler;
import io.quarkus.observation.propagation.ObservationContextStorage;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ObservationRecorder {

    private final RuntimeValue<ObservationRuntimeConfig> runtimeConfig;

    public ObservationRecorder(RuntimeValue<ObservationRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    @SuppressWarnings("unchecked")
    public Function<SyntheticCreationalContext<ObservationRegistry>, ObservationRegistry> createObservationRegistry(
            boolean hasOtelTracing) {
        return new Function<>() {
            @Override
            public ObservationRegistry apply(SyntheticCreationalContext<ObservationRegistry> context) {
                ObservationRegistry registry = ObservationRegistry.create();

                if (hasOtelTracing) {
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
        };
    }

    public void initContextStorage(BeanContainer beanContainer) {
        ObservationRegistry registry = beanContainer.beanInstance(ObservationRegistry.class);
        ObservationContextStorage.init(registry);
    }

    public void setCustomConvention() {
        InstanceHandle<ObservedInterceptorConvention> handle = Arc.container()
                .instance(ObservedInterceptorConvention.class);
        if (handle.isAvailable()) {
            ObservedInterceptor.setCustomConvention(handle.get());
        }
    }

    public void registerPrintOutHandler(BeanContainer beanContainer) {
        if (runtimeConfig.getValue().printOut()) {
            ObservationRegistry registry = beanContainer.beanInstance(ObservationRegistry.class);
            registry.observationConfig().observationHandler(new PrintOutHandler());
        }
    }

}
