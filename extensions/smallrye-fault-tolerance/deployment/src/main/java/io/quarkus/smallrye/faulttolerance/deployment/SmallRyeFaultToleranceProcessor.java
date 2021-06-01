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
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
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
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
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
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.core.util.RunnableWrapper;
import io.smallrye.faulttolerance.internal.RequestContextControllerProvider;
import io.smallrye.faulttolerance.internal.StrategyCache;
import io.smallrye.faulttolerance.metrics.MetricsProvider;
import io.smallrye.faulttolerance.propagation.ContextPropagationRequestContextControllerProvider;
import io.smallrye.faulttolerance.propagation.ContextPropagationRunnableWrapper;

public class SmallRyeFaultToleranceProcessor {

    private static final Set<DotName> FT_ANNOTATIONS = new HashSet<>();
    static {
        // @Blocking and @NonBlocking alone do _not_ trigger the fault tolerance interceptor,
        // only in combination with other fault tolerance annotations
        FT_ANNOTATIONS.add(DotName.createSimple(Asynchronous.class.getName()));
        FT_ANNOTATIONS.add(DotName.createSimple(Bulkhead.class.getName()));
        FT_ANNOTATIONS.add(DotName.createSimple(CircuitBreaker.class.getName()));
        FT_ANNOTATIONS.add(DotName.createSimple(Fallback.class.getName()));
        FT_ANNOTATIONS.add(DotName.createSimple(Retry.class.getName()));
        FT_ANNOTATIONS.add(DotName.createSimple(Timeout.class.getName()));
    }

    @BuildStep
    public void build(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            BuildProducer<FeatureBuildItem> feature, BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<BeanDefiningAnnotationBuildItem> additionalBda,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod) {

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
            additionalBean.produce(fallbackHandlersBeans.build());
        }
        // Add reflective access to fallback methods
        for (AnnotationInstance annotation : index.getAnnotations(DotName.createSimple(Fallback.class.getName()))) {
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

        for (DotName annotation : FT_ANNOTATIONS) {
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
                if (FT_ANNOTATIONS.contains(context.getTarget().asClass().name())) {
                    context.transform().add(FaultToleranceBinding.class).done();
                }
            }
        }));

        // Register bean classes
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        // Also register MP FT annotations so that they are recognized as interceptor bindings
        // Note that MP FT API jar is nor indexed, nor contains beans.xml so it is not part of the app index
        for (DotName ftAnnotation : FT_ANNOTATIONS) {
            builder.addBeanClass(ftAnnotation.toString());
        }
        builder.addBeanClasses(FaultToleranceInterceptor.class,
                ExecutorHolder.class,
                StrategyCache.class,
                QuarkusFaultToleranceOperationProvider.class,
                QuarkusFallbackHandlerProvider.class,
                QuarkusExistingCircuitBreakerNames.class,
                QuarkusAsyncExecutorProvider.class,
                MetricsProvider.class,
                CircuitBreakerMaintenanceImpl.class,
                RequestContextIntegration.class);
        additionalBean.produce(builder.build());

        if (!metricsCapability.isPresent()) {
            //disable fault tolerance metrics with the MP sys props
            systemProperty.produce(new SystemPropertyBuildItem("MP_Fault_Tolerance_Metrics_Enabled", "false"));
        }
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
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        Set<String> beanNames = new HashSet<>();
        IndexView index = beanArchiveIndexBuildItem.getIndex();

        for (BeanInfo info : validationPhase.getContext().beans()) {
            if (hasFTAnnotations(index, annotationStore, info.getImplClazz())) {
                beanNames.add(info.getBeanClass().toString());
            }
        }

        recorder.createFaultToleranceOperation(beanNames);

        DotName circuitBreakerName = DotName.createSimple(CircuitBreakerName.class.getName());

        Map<String, Set<String>> existingCircuitBreakerNames = new HashMap<>();
        for (AnnotationInstance it : index.getAnnotations(circuitBreakerName)) {
            if (it.target().kind() == Kind.METHOD) {
                MethodInfo method = it.target().asMethod();
                existingCircuitBreakerNames.computeIfAbsent(it.value().asString(), ignored -> new HashSet<>())
                        .add(method + " @ " + method.declaringClass());
            }
        }

        List<Throwable> exceptions = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : existingCircuitBreakerNames.entrySet()) {
            if (entry.getValue().size() > 1) {
                exceptions.add(new DefinitionException("Multiple circuit breakers have the same name '"
                        + entry.getKey() + "': " + entry.getValue()));
            }
        }
        if (!exceptions.isEmpty()) {
            errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(exceptions));
        }

        recorder.initExistingCircuitBreakerNames(existingCircuitBreakerNames.keySet());
    }

    private boolean hasFTAnnotations(IndexView index, AnnotationStore annotationStore, ClassInfo info) {
        if (info == null) {
            //should not happen, but guard against it
            //happens in this case due to a bug involving array types

            return false;
        }
        // first check annotations on type
        if (annotationStore.hasAnyAnnotation(info, FT_ANNOTATIONS)) {
            return true;
        }

        // then check on the methods
        for (MethodInfo method : info.methods()) {
            if (annotationStore.hasAnyAnnotation(method, FT_ANNOTATIONS)) {
                return true;
            }
        }

        // then check on the parent
        DotName parentClassName = info.superName();
        if (parentClassName == null || parentClassName.equals(DotNames.OBJECT)) {
            return false;
        }
        ClassInfo parentClassInfo = index.getClassByName(parentClassName);
        if (parentClassInfo == null) {
            return false;
        }
        return hasFTAnnotations(index, annotationStore, parentClassInfo);
    }

    @BuildStep
    public ConfigurationTypeBuildItem registerTypes() {
        return new ConfigurationTypeBuildItem(ChronoUnit.class);
    }
}
