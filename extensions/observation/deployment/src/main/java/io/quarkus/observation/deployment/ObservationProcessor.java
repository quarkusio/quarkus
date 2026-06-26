package io.quarkus.observation.deployment;

import static io.quarkus.micrometer.deployment.MicrometerProcessor.MicrometerEnabled;
import static io.quarkus.observation.deployment.ObservationProcessor.ObservationEnabled;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.observation.ObservationRecorder;
import io.quarkus.observation.cdi.ObservedInterceptor;
import io.quarkus.observation.cdi.convention.ObservedInterceptorConvention;
import io.quarkus.observation.config.ObservationBuildConfig;
import io.quarkus.observation.opentelemetry.handler.TracingAwareMeterObservationHandler;
import io.quarkus.observation.propagation.ObservationMpContextPropagationProvider;
import io.quarkus.smallrye.context.deployment.spi.ThreadContextProviderBuildItem;

@BuildSteps(onlyIf = { MicrometerEnabled.class, ObservationEnabled.class })
class ObservationProcessor {

    private static final DotName INTERCEPTOR_CLASS = DotName.createSimple(ObservedInterceptor.class.getName());
    private static final DotName OBSERVED = DotName.createSimple(Observed.class.getName());
    private static final String OBSERVATION_FEATURE = "observation";
    private static final String PROPAGATING_RECEIVER = "io.quarkus.observation.opentelemetry.handler.PropagatingReceiverTracingObservationHandler";
    private static final String PROPAGATING_SENDER = "io.quarkus.observation.opentelemetry.handler.PropagatingSenderTracingObservationHandler";
    private static final String DEFAULT_TRACING = "io.quarkus.observation.opentelemetry.handler.OpenTelemetryObservationHandler";

    public static class ObservationEnabled implements java.util.function.BooleanSupplier {

        ObservationBuildConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.enabled();
        }
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(OBSERVATION_FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerMetricsHandler() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(
                        ObservedInterceptor.class,
                        TracingAwareMeterObservationHandler.class)
                .build();
    }

    @BuildStep
    void registerOtelTracingHandlers(
            Optional<OpenTelemetrySdkBuildItem> openTelemetrySdk,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (openTelemetrySdk.isPresent() && openTelemetrySdk.get().isTracingBuildTimeEnabled()) {
            // These 3 handlers will only be present if OTel is present and enabled.
            // The handle inbound, local and outbound span generation and OTel context propagation.
            additionalBeans.produce(AdditionalBeanBuildItem.builder()
                    .setUnremovable()
                    .addBeanClass(
                            "io.quarkus.observation.opentelemetry.handler.PropagatingReceiverTracingObservationHandler")
                    .addBeanClass(
                            "io.quarkus.observation.opentelemetry.handler.PropagatingSenderTracingObservationHandler")
                    .addBeanClass(
                            "io.quarkus.observation.opentelemetry.handler.OpenTelemetryObservationHandler")
                    .build());
        }
    }

    @BuildStep
    InterceptorBindingRegistrarBuildItem registerObservedBinding() {
        return new InterceptorBindingRegistrarBuildItem(
                new InterceptorBindingRegistrar() {
                    @Override
                    public List<InterceptorBinding> getAdditionalBindings() {
                        return List.of(
                                InterceptorBinding.of(
                                        Observed.class,
                                        Set.of("name", "contextualName", "lowCardinalityKeyValues")));
                    }
                });
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformObservedAnnotations() {
        return new AnnotationsTransformerBuildItem(new AnnotationTransformation() {
            @Override
            public boolean supports(Kind kind) {
                return kind == Kind.METHOD || kind == Kind.CLASS;
            }

            @Override
            public void apply(TransformationContext ctx) {

                // Add @Observed to the interceptor
                if (ctx.declaration().kind() == Kind.CLASS) {
                    if (ctx.declaration().asClass().name().equals(INTERCEPTOR_CLASS)) {
                        ctx.add(Observed.class);
                    }
                    return;
                }

                // For every method containing the @Observed annotation set defaults in the annotation's values.
                // This will create proper metric and trace names when user has not defined those values.
                MethodInfo method = ctx.declaration().asMethod();

                AnnotationInstance observed = null;
                for (AnnotationInstance ann : ctx.annotations()) {
                    if (ann.name().equals(OBSERVED)) {
                        observed = ann;
                        break;
                    }
                }
                if (observed == null) {
                    return;
                }

                // Set the @Observed.name if absent.
                AnnotationValue nameVal = observed.value("name");
                String name = (nameVal != null && !nameVal.asString().isEmpty())
                        ? nameVal.asString()
                        : method.name();

                // Set the @Observed.contextualName if absent.
                AnnotationValue ctxVal = observed.value("contextualName");
                String ctxName = (ctxVal != null && !ctxVal.asString().isEmpty())
                        ? ctxVal.asString()
                        : method.declaringClass().simpleName() + "#" + method.name();

                // Preserve original lowCardinalityKeyValues in the new annotation instance
                AnnotationValue kvVal = observed.value("lowCardinalityKeyValues");
                AnnotationValue[] kvElements;
                if (kvVal != null && kvVal.asStringArray().length > 0) {
                    String[] kvs = kvVal.asStringArray();
                    kvElements = new AnnotationValue[kvs.length];
                    for (int i = 0; i < kvs.length; i++) {
                        kvElements[i] = AnnotationValue.createStringValue("", kvs[i]);
                    }
                } else {
                    kvElements = new AnnotationValue[0];
                }

                // Example before
                // 0 = {AnnotationValue$StringValue@7930} "name = "custom.name""
                ctx.remove(ann -> ann.name().equals(OBSERVED));
                // Example after
                //  0 = {AnnotationValue$StringValue@7958} "contextualName = "ObservedBean#customNameMethod""
                //  1 = {AnnotationValue$ArrayValue@7959} "lowCardinalityKeyValues = []"
                //  2 = {AnnotationValue$StringValue@7960} "name = "custom.name""
                ctx.add(AnnotationInstance.create(OBSERVED, method,
                        new AnnotationValue[] {
                                AnnotationValue.createStringValue("name", name),
                                AnnotationValue.createStringValue("contextualName", ctxName),
                                AnnotationValue.createArrayValue("lowCardinalityKeyValues", kvElements)
                        }));
            }
        });
    }

    @BuildStep
    UnremovableBeanBuildItem ensureUserExtensionClassesRetained() {
        return UnremovableBeanBuildItem.beanTypes(
                ObservationHandler.class,
                ObservationFilter.class,
                ObservationPredicate.class,
                GlobalObservationConvention.class,
                MeterObservationHandler.class,
                ObservedInterceptorConvention.class);
    }

    /**
     * Create the Observation Registry after all handlers and user extension classes are available.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem observationRegistryBean(
            ObservationRecorder recorder,
            Optional<OpenTelemetrySdkBuildItem> openTelemetrySdk) {
        boolean tracingEnabled = openTelemetrySdk.isPresent()
                && openTelemetrySdk.get().isTracingBuildTimeEnabled();
        var builder = SyntheticBeanBuildItem.configure(ObservationRegistry.class)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .addInjectionPoint(ClassType.create(
                        DotName.createSimple(TracingAwareMeterObservationHandler.class)));
        if (tracingEnabled) {
            builder.addInjectionPoint(ClassType.create(DotName.createSimple(PROPAGATING_RECEIVER)))
                    .addInjectionPoint(ClassType.create(DotName.createSimple(PROPAGATING_SENDER)))
                    .addInjectionPoint(ClassType.create(DotName.createSimple(DEFAULT_TRACING)));
        }
        return builder.createWith(recorder.createObservationRegistry(tracingEnabled)).done();
    }

    /**
     * This will set the Observation registry, a synthetic bean, in the ObservationContextStorage
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    void initContextStorage(ObservationRecorder recorder, BeanContainerBuildItem beanContainer) {
        recorder.initContextStorage(beanContainer.getValue());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    void resolveCustomConvention(ObservationRecorder recorder, CombinedIndexBuildItem combinedIndex) {
        DotName conventionName = DotName.createSimple(
                "io.quarkus.observation.cdi.convention.ObservedInterceptorConvention");
        DotName defaultConventionName = DotName.createSimple(
                "io.quarkus.observation.cdi.convention.DefaultObservedInterceptorConvention");
        Collection<ClassInfo> implementors = combinedIndex.getIndex().getAllKnownImplementors(conventionName);
        boolean hasCustom = implementors.stream().anyMatch(ci -> !ci.name().equals(defaultConventionName));
        if (hasCustom) {
            recorder.setCustomConvention();
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    void registerPrintOutHandler(ObservationRecorder recorder, BeanContainerBuildItem beanContainer) {
        recorder.registerPrintOutHandler(beanContainer.getValue());
    }

    @BuildStep
    void registerContextPropagation(
            BuildProducer<ThreadContextProviderBuildItem> threadContextProvider) {
        threadContextProvider.produce(
                new ThreadContextProviderBuildItem(ObservationMpContextPropagationProvider.class));
    }
}
