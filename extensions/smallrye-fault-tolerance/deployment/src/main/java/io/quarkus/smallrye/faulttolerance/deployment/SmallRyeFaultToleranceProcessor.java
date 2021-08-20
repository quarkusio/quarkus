package io.quarkus.smallrye.faulttolerance.deployment;

import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.DefinitionException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
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
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusAsyncExecutorProvider;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusExistingCircuitBreakerNames;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFallbackHandlerProvider;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFaultToleranceOperationProvider;
import io.quarkus.smallrye.faulttolerance.runtime.SmallRyeFaultToleranceRecorder;
import io.smallrye.faulttolerance.CircuitBreakerMaintenanceImpl;
import io.smallrye.faulttolerance.ExecutorHolder;
import io.smallrye.faulttolerance.FaultToleranceBinding;
import io.smallrye.faulttolerance.FaultToleranceInterceptor;
import io.smallrye.faulttolerance.RequestContextIntegration;
import io.smallrye.faulttolerance.SpecCompatibility;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.core.util.RunnableWrapper;
import io.smallrye.faulttolerance.internal.RequestContextControllerProvider;
import io.smallrye.faulttolerance.internal.StrategyCache;
import io.smallrye.faulttolerance.metrics.MetricsProvider;
import io.smallrye.faulttolerance.propagation.ContextPropagationRequestContextControllerProvider;
import io.smallrye.faulttolerance.propagation.ContextPropagationRunnableWrapper;

public class SmallRyeFaultToleranceProcessor {

    @BuildStep
    public void build(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            BuildProducer<FeatureBuildItem> feature, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<BeanDefiningAnnotationBuildItem> additionalBda,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> config) {

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_FAULT_TOLERANCE));

        serviceProvider.produce(new ServiceProviderBuildItem(RequestContextControllerProvider.class.getName(),
                ContextPropagationRequestContextControllerProvider.class.getName()));
        serviceProvider.produce(new ServiceProviderBuildItem(RunnableWrapper.class.getName(),
                ContextPropagationRunnableWrapper.class.getName()));

        IndexView index = combinedIndexBuildItem.getIndex();

        // Add reflective acccess to fallback handlers
        Set<String> fallbackHandlers = new HashSet<>();
        for (ClassInfo implementor : index
                .getAllKnownImplementors(DotName.createSimple(FallbackHandler.class.getName()))) {
            fallbackHandlers.add(implementor.name().toString());
        }
        if (!fallbackHandlers.isEmpty()) {
            AdditionalBeanBuildItem.Builder fallbackHandlersBeans = AdditionalBeanBuildItem.builder()
                    .setDefaultScope(BuiltinScope.DEPENDENT.getName());
            for (String fallbackHandler : fallbackHandlers) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, fallbackHandler));
                fallbackHandlersBeans.addBeanClass(fallbackHandler);
            }
            beans.produce(fallbackHandlersBeans.build());
        }
        // Add reflective access to fallback methods
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.FALLBACK)) {
            AnnotationValue fallbackMethodValue = annotation.value("fallbackMethod");
            if (fallbackMethodValue == null) {
                continue;
            }
            String fallbackMethod = fallbackMethodValue.asString();

            Queue<DotName> classesToScan = new ArrayDeque<>(); // work queue

            // @Fallback can only be present on methods, so this is just future-proofing
            AnnotationTarget target = annotation.target();
            if (target.kind() == Kind.METHOD) {
                classesToScan.add(target.asMethod().declaringClass().name());
            }

            while (!classesToScan.isEmpty()) {
                DotName name = classesToScan.poll();
                ClassInfo clazz = index.getClassByName(name);
                if (clazz == null) {
                    continue;
                }

                // we could further restrict the set of registered methods based on matching parameter types,
                // but that's relatively complex and SmallRye Fault Tolerance has to do it anyway
                clazz.methods()
                        .stream()
                        .filter(it -> fallbackMethod.equals(it.name()))
                        .forEach(it -> reflectiveMethod.produce(new ReflectiveMethodBuildItem(it)));

                DotName superClass = clazz.superName();
                if (superClass != null && !DotNames.OBJECT.equals(superClass)) {
                    classesToScan.add(superClass);
                }
                classesToScan.addAll(clazz.interfaceNames());
            }
        }
        // Add reflective access to custom backoff strategies
        for (ClassInfo strategy : index.getAllKnownImplementors(DotNames.CUSTOM_BACKOFF_STRATEGY)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, strategy.name().toString()));
        }

        for (DotName annotation : DotNames.FT_ANNOTATIONS) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, annotation.toString()));
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
                        QuarkusAsyncExecutorProvider.class,
                        MetricsProvider.class,
                        CircuitBreakerMaintenanceImpl.class,
                        RequestContextIntegration.class,
                        SpecCompatibility.class);
        beans.produce(builder.build());

        // TODO FT should be smart enough and only initialize the stuff in the recorder if it's really needed
        // The FaultToleranceInterceptor needs to be registered as unremovable due to the rest-client integration - interceptors 
        // are currently resolved dynamically at runtime because per the spec interceptor bindings cannot be declared on interfaces
        beans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(FaultToleranceInterceptor.class, QuarkusFaultToleranceOperationProvider.class,
                        QuarkusExistingCircuitBreakerNames.class)
                .build());

        if (!metricsCapability.isPresent()) {
            //disable fault tolerance metrics with the MP sys props
            systemProperty.produce(new SystemPropertyBuildItem("MP_Fault_Tolerance_Metrics_Enabled", "false"));
        }

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
                    if (!ctx.getTarget().asClass().name().toString()
                            .equals("io.smallrye.faulttolerance.FaultToleranceInterceptor")) {
                        return;
                    }
                    final Config config = ConfigProvider.getConfig();

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
    void validateFaultToleranceAnnotations(SmallRyeFaultToleranceRecorder recorder,
            ValidationPhaseBuildItem validationPhase,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            AnnotationProxyBuildItem annotationProxy,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {

        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        IndexView index = beanArchiveIndexBuildItem.getIndex();
        // only generating annotation literal classes for MicroProfile/SmallRye Fault Tolerance annotations,
        // none of them are application classes
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, false);

        FaultToleranceScanner scaner = new FaultToleranceScanner(index, annotationStore, annotationProxy, classOutput);

        List<FaultToleranceMethod> ftMethods = new ArrayList<>();
        List<Throwable> exceptions = new ArrayList<>();

        for (BeanInfo info : validationPhase.getContext().beans()) {
            ClassInfo beanClass = info.getImplClazz();
            if (beanClass == null) {
                continue;
            }

            if (scaner.hasFTAnnotations(beanClass)) {
                scaner.forEachMethod(beanClass, method -> {
                    FaultToleranceMethod ftMethod = scaner.createFaultToleranceMethod(beanClass, method);
                    if (ftMethod.isLegitimate()) {
                        ftMethods.add(ftMethod);

                        if (method.hasAnnotation(DotNames.BLOCKING) && method.hasAnnotation(DotNames.NON_BLOCKING)) {
                            exceptions.add(
                                    new DefinitionException("Both @Blocking and @NonBlocking present on '" + method + "'"));
                        }
                    }
                });

                if (beanClass.classAnnotation(DotNames.BLOCKING) != null
                        && beanClass.classAnnotation(DotNames.NON_BLOCKING) != null) {
                    exceptions.add(new DefinitionException("Both @Blocking and @NonBlocking present on '" + beanClass + "'"));
                }
            }
        }

        recorder.createFaultToleranceOperation(ftMethods);

        Map<String, Set<String>> existingCircuitBreakerNames = new HashMap<>();
        for (AnnotationInstance it : index.getAnnotations(DotNames.CIRCUIT_BREAKER_NAME)) {
            if (it.target().kind() == Kind.METHOD) {
                MethodInfo method = it.target().asMethod();
                existingCircuitBreakerNames.computeIfAbsent(it.value().asString(), ignored -> new HashSet<>())
                        .add(method + " @ " + method.declaringClass());
            }
        }
        for (Map.Entry<String, Set<String>> entry : existingCircuitBreakerNames.entrySet()) {
            if (entry.getValue().size() > 1) {
                exceptions.add(new DefinitionException("Multiple circuit breakers have the same name '"
                        + entry.getKey() + "': " + entry.getValue()));
            }
        }

        for (DotName backoffAnnotation : DotNames.BACKOFF_ANNOTATIONS) {
            for (AnnotationInstance it : index.getAnnotations(backoffAnnotation)) {
                if (it.target().kind() == Kind.CLASS && it.target().asClass().classAnnotation(DotNames.RETRY) == null) {
                    exceptions.add(new DefinitionException("Backoff annotation @" + backoffAnnotation.withoutPackagePrefix()
                            + " present on '" + it.target() + "', but @Retry is missing"));
                } else if (it.target().kind() == Kind.METHOD && !it.target().asMethod().hasAnnotation(DotNames.RETRY)) {
                    exceptions.add(new DefinitionException("Backoff annotation @" + backoffAnnotation.withoutPackagePrefix()
                            + " present on '" + it.target() + "', but @Retry is missing"));
                }
            }
        }

        if (!exceptions.isEmpty()) {
            errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(exceptions));
        }

        recorder.initExistingCircuitBreakerNames(existingCircuitBreakerNames.keySet());
    }

    @BuildStep
    public ConfigurationTypeBuildItem registerTypes() {
        return new ConfigurationTypeBuildItem(ChronoUnit.class);
    }
}
