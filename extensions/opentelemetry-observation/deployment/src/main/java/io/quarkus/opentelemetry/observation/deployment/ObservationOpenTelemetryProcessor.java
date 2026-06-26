package io.quarkus.opentelemetry.observation.deployment;

import java.util.List;
import java.util.Set;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;
import io.quarkus.opentelemetry.observation.ObservationOpenTelemetryRecorder;
import io.quarkus.opentelemetry.observation.cdi.ObservedInterceptor;
import io.quarkus.opentelemetry.observation.handler.OpenTelemetryObservationHandler;
import io.quarkus.opentelemetry.observation.handler.PropagatingReceiverTracingObservationHandler;
import io.quarkus.opentelemetry.observation.handler.PropagatingSenderTracingObservationHandler;
import io.quarkus.opentelemetry.observation.handler.TracingAwareMeterObservationHandler;
import io.quarkus.opentelemetry.observation.propagation.ObservationMpContextPropagationProvider;
import io.quarkus.smallrye.context.deployment.spi.ThreadContextProviderBuildItem;

@BuildSteps(onlyIf = { OpenTelemetryEnabled.class, ObservationOpenTelemetryEnabled.class })
class ObservationOpenTelemetryProcessor {

    private static final String FEATURE = "opentelemetry-observation";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerHandlerBeans() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(
                        PropagatingReceiverTracingObservationHandler.class,
                        PropagatingSenderTracingObservationHandler.class,
                        OpenTelemetryObservationHandler.class,
                        TracingAwareMeterObservationHandler.class,
                        ObservedInterceptor.class)
                .build();
    }

    @BuildStep
    InterceptorBindingRegistrarBuildItem registerObservedBinding() {
        return new InterceptorBindingRegistrarBuildItem(
                new InterceptorBindingRegistrar() {
                    @Override
                    public List<InterceptorBinding> getAdditionalBindings() {
                        return List.of(InterceptorBinding.of(Observed.class,
                                Set.of("name", "contextualName", "lowCardinalityKeyValues")));
                    }
                });
    }

    @BuildStep
    UnremovableBeanBuildItem ensureUserExtensionsRetained() {
        return UnremovableBeanBuildItem.beanTypes(
                ObservationHandler.class,
                ObservationFilter.class,
                ObservationPredicate.class,
                GlobalObservationConvention.class,
                MeterObservationHandler.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem observationRegistryBean(
            ObservationOpenTelemetryRecorder recorder) {
        return SyntheticBeanBuildItem.configure(ObservationRegistry.class)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .createWith(recorder.createObservationRegistry())
                .done();
    }

    @BuildStep
    void registerContextPropagation(
            BuildProducer<ThreadContextProviderBuildItem> threadContextProvider) {
        threadContextProvider.produce(
                new ThreadContextProviderBuildItem(ObservationMpContextPropagationProvider.class));
    }
}
