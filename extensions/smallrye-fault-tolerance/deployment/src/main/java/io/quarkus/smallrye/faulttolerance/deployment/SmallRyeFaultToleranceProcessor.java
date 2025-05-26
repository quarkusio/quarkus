package io.quarkus.smallrye.faulttolerance.deployment;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.faulttolerance.deployment.devui.FaultToleranceInfoBuildItem;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusAsyncExecutorProvider;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusBeforeRetryHandlerProvider;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusExistingCircuitBreakerNames;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFallbackHandlerProvider;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFaultToleranceOperationProvider;
import io.quarkus.smallrye.faulttolerance.runtime.SmallRyeFaultToleranceRecorder;
import io.quarkus.smallrye.faulttolerance.runtime.config.SmallRyeFaultToleranceConfigRelocate;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.faulttolerance.CdiSpi;
import io.smallrye.faulttolerance.CircuitBreakerMaintenanceImpl;
import io.smallrye.faulttolerance.Enablement;
import io.smallrye.faulttolerance.ExecutorHolder;
import io.smallrye.faulttolerance.FaultToleranceBinding;
import io.smallrye.faulttolerance.FaultToleranceInterceptor;
import io.smallrye.faulttolerance.RequestContextIntegration;
import io.smallrye.faulttolerance.SpecCompatibility;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.core.util.RunnableWrapper;
import io.smallrye.faulttolerance.internal.RequestContextControllerProvider;
import io.smallrye.faulttolerance.internal.StrategyCache;
import io.smallrye.faulttolerance.propagation.ContextPropagationRequestContextControllerProvider;
import io.smallrye.faulttolerance.propagation.ContextPropagationRunnableWrapper;

public class SmallRyeFaultToleranceProcessor {

    @BuildStep
    public void build(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            BuildProducer<FeatureBuildItem> feature, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<BeanDefiningAnnotationBuildItem> additionalBda,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            Optional<OpenTelemetrySdkBuildItem> openTelemetrySdk,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClassBuildItems) {

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_FAULT_TOLERANCE));

        serviceProvider.produce(new ServiceProviderBuildItem(RequestContextControllerProvider.class.getName(),
                ContextPropagationRequestContextControllerProvider.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(RunnableWrapper.class.getName(),
                ContextPropagationRunnableWrapper.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(ConfigSourceInterceptor.class.getName(),
                SmallRyeFaultToleranceConfigRelocate.class.getName()));
        // make sure this is initialised at runtime, otherwise it will get a non-initialised ContextPropagationManager
        runtimeInitializedClassBuildItems.produce(new RuntimeInitializedClassBuildItem(RunnableWrapper.class.getName()));

        IndexView index = combinedIndexBuildItem.getIndex();

        // Add reflective access to fallback handlers and before retry handlers
        // (reflective access to fallback methods and before retry methods is added
        // in `FaultToleranceScanner.searchForMethods`)
        Set<String> handlers = new HashSet<>();
        for (ClassInfo implementor : index.getAllKnownImplementors(DotNames.FALLBACK_HANDLER)) {
            handlers.add(implementor.name().toString());
        }
        for (ClassInfo implementor : index.getAllKnownImplementors(DotNames.BEFORE_RETRY_HANDLER)) {
            handlers.add(implementor.name().toString());
        }
        if (!handlers.isEmpty()) {
            AdditionalBeanBuildItem.Builder handlerBeans = AdditionalBeanBuildItem.builder()
                    .setDefaultScope(BuiltinScope.DEPENDENT.getName());
            for (String handler : handlers) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(handler).methods().build());
                handlerBeans.addBeanClass(handler);
            }
            beans.produce(handlerBeans.build());
        }
        // Add reflective access to custom backoff strategies
        for (ClassInfo strategy : index.getAllKnownImplementors(DotNames.CUSTOM_BACKOFF_STRATEGY)) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(strategy.name().toString()).methods().build());
        }
        // Add reflective access to retry predicates
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.RETRY_WHEN)) {
            for (String memberName : List.of("result", "exception")) {
                AnnotationValue member = annotation.value(memberName);
                if (member != null) {
                    reflectiveClass.produce(ReflectiveClassBuildItem.builder(member.asClass().name().toString()).build());
                }
            }
        }

        for (DotName annotation : DotNames.FT_ANNOTATIONS) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(annotation.toString()).methods().build());
            // also make them bean defining annotations
            additionalBda.produce(new BeanDefiningAnnotationBuildItem(annotation));
        }

        // Add transitive interceptor binding to FT annotations
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (DotNames.FT_ANNOTATIONS.contains(context.getTarget().asClass().name())) {
                    context.transform().add(FaultToleranceBinding.class).done();
                }
            }
        }));

        // Register bean classes
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        // Also register MP FT annotations so that they are recognized as interceptor bindings
        // Note that MP FT API jar is nor indexed, nor contains beans.xml so it is not part of the app index
        for (DotName ftAnnotation : DotNames.FT_ANNOTATIONS) {
            builder.addBeanClass(ftAnnotation.toString());
        }
        builder
                .addBeanClasses(
                        ExecutorHolder.class,
                        StrategyCache.class,
                        QuarkusFallbackHandlerProvider.class,
                        QuarkusBeforeRetryHandlerProvider.class,
                        QuarkusAsyncExecutorProvider.class,
                        CircuitBreakerMaintenanceImpl.class,
                        RequestContextIntegration.class,
                        SpecCompatibility.class,
                        Enablement.class);

        int metricsProviders = 0;
        if (metricsCapability.isPresent() && metricsCapability.get().metricsSupported(MetricsFactory.MP_METRICS)) {
            builder.addBeanClass("io.smallrye.faulttolerance.metrics.MicroProfileMetricsProvider");
            metricsProviders++;
        } else if (metricsCapability.isPresent() && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
            builder.addBeanClass("io.smallrye.faulttolerance.metrics.MicrometerProvider");
            metricsProviders++;
        }
        if (openTelemetrySdk.map(OpenTelemetrySdkBuildItem::isMetricsBuildTimeEnabled).orElse(false)) {
            builder.addBeanClass("io.smallrye.faulttolerance.metrics.OpenTelemetryProvider");
            metricsProviders++;
        }

        if (metricsProviders == 0) {
            builder.addBeanClass("io.smallrye.faulttolerance.metrics.NoopProvider");
        } else if (metricsProviders > 1) {
            builder.addBeanClass("io.smallrye.faulttolerance.metrics.CompoundMetricsProvider");
        }

        beans.produce(builder.build());

        // TODO FT should be smart enough and only initialize the stuff in the recorder if it's really needed
        // The FaultToleranceInterceptor needs to be registered as unremovable due to the rest-client integration - interceptors
        // are currently resolved dynamically at runtime because per the spec interceptor bindings cannot be declared on interfaces
        beans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(FaultToleranceInterceptor.class, QuarkusFaultToleranceOperationProvider.class,
                        QuarkusExistingCircuitBreakerNames.class, CdiSpi.EagerDependencies.class,
                        CdiSpi.LazyDependencies.class)
                .build());

        config.produce(new RunTimeConfigurationDefaultBuildItem("smallrye.faulttolerance.mp-compatibility", "false"));
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformInterceptorPriority(BeanArchiveIndexBuildItem index) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (ctx.isClass()) {
                    if (!ctx.getTarget().asClass().name().equals(DotNames.FAULT_TOLERANCE_INTERCEPTOR)) {
                        return;
                    }
                    Config config = ConfigProvider.getConfig();

                    OptionalInt priority = config.getValue("mp.fault.tolerance.interceptor.priority", OptionalInt.class);
                    if (priority.isPresent()) {
                        ctx.transform()
                                .remove(ann -> ann.name().toString().equals(Priority.class.getName()))
                                .add(Priority.class, AnnotationValue.createIntegerValue("value", priority.getAsInt()))
                                .done();
                    }
                }
            }
        });
    }

    @BuildStep
    // needs to be RUNTIME_INIT because we need to read MP Config
    @Record(ExecutionTime.RUNTIME_INIT)
    void processFaultToleranceAnnotations(SmallRyeFaultToleranceRecorder recorder,
            RecorderContext recorderContext,
            ValidationPhaseBuildItem validationPhase,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            AnnotationProxyBuildItem annotationProxy,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors,
            BuildProducer<FaultToleranceInfoBuildItem> faultToleranceInfo) {

        Config config = ConfigProvider.getConfig();

        Set<String> exceptionConfigs = Set.of("CircuitBreaker/failOn", "CircuitBreaker/skipOn",
                "Fallback/applyOn", "Fallback/skipOn", "Retry/retryOn", "Retry/abortOn");

        for (String exceptionConfig : exceptionConfigs) {
            Optional<String[]> exceptionNames = config.getOptionalValue(exceptionConfig, String[].class);
            if (exceptionNames.isPresent()) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(exceptionNames.get())
                        .reason(getClass().getName()).build());
            }
        }

        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        IndexView index = beanArchiveIndexBuildItem.getIndex();
        // only generating annotation literal classes for MicroProfile/SmallRye Fault Tolerance annotations,
        // none of them are application classes
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, false);

        FaultToleranceScanner scanner = new FaultToleranceScanner(index, annotationStore, annotationProxy, classOutput,
                recorderContext, reflectiveMethod);

        List<FaultToleranceMethod> ftMethods = new ArrayList<>();
        List<Throwable> exceptions = new ArrayList<>();
        Map<String, Set<String>> existingCircuitBreakerNames = new HashMap<>();

        Map<String, Set<String>> existingGuards = new HashMap<>();
        Set<String> expectedGuards = new HashSet<>();

        for (BeanInfo info : validationPhase.getContext().beans()) {
            if (info.hasType(DotNames.GUARD) || info.hasType(DotNames.TYPED_GUARD)) {
                info.getQualifier(DotNames.IDENTIFIER).ifPresent(idAnn -> {
                    String id = idAnn.value().asString();
                    existingGuards.computeIfAbsent(id, ignored -> new HashSet<>()).add(info.toString());
                    if ("global".equals(id)) {
                        exceptions.add(new DefinitionException("Guard/TypedGuard with identifier 'global' is not allowed: "
                                + info));
                    }
                });
            }

            ClassInfo beanClass = info.getImplClazz();
            if (beanClass == null) {
                continue;
            }

            if (scanner.hasFTAnnotations(beanClass)) {
                for (String exceptionConfig : exceptionConfigs) {
                    Optional<String[]> exceptionNames = config.getOptionalValue(beanClass.name().toString()
                            + "/" + exceptionConfig, String[].class);
                    if (exceptionNames.isPresent()) {
                        reflectiveClass.produce(ReflectiveClassBuildItem.builder(exceptionNames.get())
                                .reason(getClass().getName()).build());
                    }
                }

                scanner.forEachMethod(beanClass, method -> {
                    FaultToleranceMethod ftMethod = scanner.createFaultToleranceMethod(beanClass, method);
                    if (ftMethod.isLegitimate()) {
                        ftMethods.add(ftMethod);

                        reflectiveMethod.produce(new ReflectiveMethodBuildItem("fault tolerance method", method));

                        if (annotationStore.hasAnnotation(method, DotNames.ASYNCHRONOUS)
                                && annotationStore.hasAnnotation(method, DotNames.ASYNCHRONOUS_NON_BLOCKING)) {
                            exceptions.add(new DefinitionException(
                                    "Both @Asynchronous and @AsynchronousNonBlocking present on '" + method + "'"));
                        }

                        if (annotationStore.hasAnnotation(method, DotNames.BLOCKING)
                                && annotationStore.hasAnnotation(method, DotNames.NON_BLOCKING)) {
                            exceptions.add(
                                    new DefinitionException("Both @Blocking and @NonBlocking present on '" + method + "'"));
                        }

                        for (String exceptionConfig : exceptionConfigs) {
                            Optional<String[]> exceptionNames = config.getOptionalValue(beanClass.name().toString()
                                    + "/" + method.name() + "/" + exceptionConfig, String[].class);
                            if (exceptionNames.isPresent()) {
                                reflectiveClass.produce(ReflectiveClassBuildItem.builder(exceptionNames.get())
                                        .reason(getClass().getName()).build());
                            }
                        }

                        if (annotationStore.hasAnnotation(method, DotNames.CIRCUIT_BREAKER_NAME)) {
                            AnnotationInstance ann = annotationStore.getAnnotation(method, DotNames.CIRCUIT_BREAKER_NAME);
                            existingCircuitBreakerNames.computeIfAbsent(ann.value().asString(), ignored -> new HashSet<>())
                                    .add(method + " @ " + method.declaringClass());
                        }

                        if (annotationStore.hasAnnotation(method, DotNames.APPLY_GUARD)) {
                            expectedGuards.add(annotationStore.getAnnotation(method, DotNames.APPLY_GUARD).value().asString());
                        }
                    }
                });

                if (annotationStore.hasAnnotation(beanClass, DotNames.ASYNCHRONOUS)
                        && annotationStore.hasAnnotation(beanClass, DotNames.ASYNCHRONOUS_NON_BLOCKING)) {
                    exceptions.add(new DefinitionException(
                            "Both @Asynchronous and @AsynchronousNonBlocking present on '" + beanClass + "'"));
                }

                if (annotationStore.hasAnnotation(beanClass, DotNames.BLOCKING)
                        && annotationStore.hasAnnotation(beanClass, DotNames.NON_BLOCKING)) {
                    exceptions.add(new DefinitionException("Both @Blocking and @NonBlocking present on '" + beanClass + "'"));
                }

                if (annotationStore.hasAnnotation(beanClass, DotNames.APPLY_GUARD)) {
                    expectedGuards.add(annotationStore.getAnnotation(beanClass, DotNames.APPLY_GUARD).value().asString());
                }
            }
        }

        recorder.createFaultToleranceOperation(ftMethods);

        for (Map.Entry<String, Set<String>> entry : existingCircuitBreakerNames.entrySet()) {
            if (entry.getValue().size() > 1) {
                exceptions.add(new DefinitionException("Multiple circuit breakers have the same name '"
                        + entry.getKey() + "': " + entry.getValue()));
            }
        }

        for (DotName backoffAnnotation : DotNames.BACKOFF_ANNOTATIONS) {
            for (AnnotationInstance it : index.getAnnotations(backoffAnnotation)) {
                if (!annotationStore.hasAnnotation(it.target(), DotNames.RETRY)) {
                    exceptions.add(new DefinitionException("Backoff annotation @" + backoffAnnotation.withoutPackagePrefix()
                            + " present on '" + it.target() + "', but @Retry is missing"));
                }
            }
        }
        for (AnnotationInstance it : index.getAnnotations(DotNames.RETRY_WHEN)) {
            if (!annotationStore.hasAnnotation(it.target(), DotNames.RETRY)) {
                exceptions.add(new DefinitionException("@RetryWhen present on '" + it.target() + "', but @Retry is missing"));
            }
        }
        for (AnnotationInstance it : index.getAnnotations(DotNames.BEFORE_RETRY)) {
            if (!annotationStore.hasAnnotation(it.target(), DotNames.RETRY)) {
                exceptions.add(new DefinitionException("@BeforeRetry present on '" + it.target() + "', but @Retry is missing"));
            }
        }

        for (Map.Entry<String, Set<String>> entry : existingGuards.entrySet()) {
            if (entry.getValue().size() > 1) {
                exceptions.add(new DefinitionException("Multiple Guard/TypedGuard beans have the same identifier '"
                        + entry.getKey() + "': " + entry.getValue()));
            }
        }
        for (String expectedGuard : expectedGuards) {
            if (!existingGuards.containsKey(expectedGuard)) {
                exceptions.add(new DefinitionException("Guard/TypedGuard with identifier '" + expectedGuard
                        + "' expected, but does not exist"));
            }
        }

        if (!exceptions.isEmpty()) {
            errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(exceptions));
        }

        recorder.initExistingCircuitBreakerNames(existingCircuitBreakerNames.keySet());

        // dev UI
        faultToleranceInfo.produce(new FaultToleranceInfoBuildItem(ftMethods.size()));
    }

    @BuildStep
    public ConfigurationTypeBuildItem registerTypes() {
        return new ConfigurationTypeBuildItem(ChronoUnit.class);
    }
}
