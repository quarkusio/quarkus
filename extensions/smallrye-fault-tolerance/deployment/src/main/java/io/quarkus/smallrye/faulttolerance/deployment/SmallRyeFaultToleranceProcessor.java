package io.quarkus.smallrye.faulttolerance.deployment;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.jboss.jandex.*;
import org.jboss.jandex.AnnotationTarget.Kind;

import com.netflix.hystrix.HystrixCircuitBreaker;

import io.quarkus.arc.deployment.*;
import io.quarkus.arc.processor.*;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFallbackHandlerProvider;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFaultToleranceOperationProvider;
import io.quarkus.smallrye.faulttolerance.runtime.RequestContextListener;
import io.quarkus.smallrye.faulttolerance.runtime.SmallryeFaultToleranceRecorder;
import io.smallrye.faulttolerance.DefaultCommandListenersProvider;
import io.smallrye.faulttolerance.DefaultHystrixConcurrencyStrategy;
import io.smallrye.faulttolerance.HystrixCommandBinding;
import io.smallrye.faulttolerance.HystrixCommandInterceptor;
import io.smallrye.faulttolerance.HystrixInitializer;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.metrics.MetricsCollectorFactory;

public class SmallRyeFaultToleranceProcessor {
    private static final DotName ASYNCHRONOUS = DotName.createSimple(Asynchronous.class.getName());
    private static final DotName BULKHEAD = DotName.createSimple(Bulkhead.class.getName());
    private static final DotName CIRCUIT_BREAKER = DotName.createSimple(CircuitBreaker.class.getName());
    private static final DotName FALLBACK = DotName.createSimple(Fallback.class.getName());
    private static final DotName RETRY = DotName.createSimple(Retry.class.getName());
    private static final DotName TIMEOUT = DotName.createSimple(Timeout.class.getName());

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<SubstrateSystemPropertyBuildItem> nativeImageSystemProperty;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    SubstrateSystemPropertyBuildItem disableJmx() {
        return new SubstrateSystemPropertyBuildItem("archaius.dynamicPropertyFactory.registerConfigWithJMX", "false");
    }

    @BuildStep
    public void build(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            BuildProducer<FeatureBuildItem> feature, BuildProducer<AdditionalBeanBuildItem> additionalBean) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_FAULT_TOLERANCE));

        Set<DotName> ftAnnotations = new HashSet<>();
        ftAnnotations.add(DotName.createSimple(Asynchronous.class.getName()));
        ftAnnotations.add(DotName.createSimple(Bulkhead.class.getName()));
        ftAnnotations.add(DotName.createSimple(CircuitBreaker.class.getName()));
        ftAnnotations.add(DotName.createSimple(Fallback.class.getName()));
        ftAnnotations.add(DotName.createSimple(Retry.class.getName()));
        ftAnnotations.add(DotName.createSimple(Timeout.class.getName()));

        IndexView index = combinedIndexBuildItem.getIndex();

        // Make sure rx.internal.util.unsafe.UnsafeAccess.DISABLED_BY_USER is set.
        nativeImageSystemProperty.produce(new SubstrateSystemPropertyBuildItem("rx.unsafe-disable", "true"));

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

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, HystrixCircuitBreaker.Factory.class.getName()));
        for (DotName annotation : ftAnnotations) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, annotation.toString()));
        }

        // Add transitive interceptor binding to FT annotations
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (ftAnnotations.contains(context.getTarget().asClass().name())) {
                    context.transform().add(HystrixCommandBinding.class).done();
                }
            }
        }));

        // Register bean classes
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        // Also register MP FT annotations so that they are recognized as interceptor bindings
        // Note that MP FT API jar is nor indexed, nor contains beans.xml so it is not part of the app index
        for (DotName ftAnnotation : ftAnnotations) {
            builder.addBeanClass(ftAnnotation.toString());
        }
        builder.addBeanClasses(HystrixCommandInterceptor.class, HystrixInitializer.class,
                DefaultHystrixConcurrencyStrategy.class,
                QuarkusFaultToleranceOperationProvider.class, QuarkusFallbackHandlerProvider.class,
                DefaultCommandListenersProvider.class,
                MetricsCollectorFactory.class,
                RequestContextListener.class);
        additionalBean.produce(builder.build());
    }

    @BuildStep
    void validateFaultToleranceAnnotations(
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errorBuildItemProducer,
            ValidationPhaseBuildItem validationPhase) {
        for (BeanInfo bean : validationPhase.getContext().get(BuildExtension.Key.BEANS)) {
            if (bean.isClassBean()) {
                try {
                    Class<?> beanClass = Class.forName(bean.getBeanClass().toString());

                    for (Method method : beanClass.getDeclaredMethods()) {
                        FaultToleranceOperation operation = FaultToleranceOperation.of(beanClass, method);
                        if (operation.isLegitimate()) {
                            try {
                                operation.validate();
                            } catch (FaultToleranceDefinitionException e) {
                                errorBuildItemProducer.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(e));
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // Ignore
                }
            }
        }
    }

    //    @BuildStep
    //    AnnotationsTransformerBuildItem addActivateRequestContextAnnotation(BeanArchiveIndexBuildItem index) {
    //        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
    //            @Override
    //            public boolean appliesTo(AnnotationTarget.Kind kind) {
    //                return kind == Kind.CLASS || kind == Kind.METHOD;
    //            }
    //
    //            @Override
    //            public void transform(TransformationContext ctx) {
    //                if (ctx.isClass()) {
    //                    ClassInfo clazz = ctx.getTarget().asClass();
    //                    while (clazz != null && clazz.superName() != null) {
    //                        Map<DotName, List<AnnotationInstance>> annotations = clazz.annotations();
    //                        if (annotations.containsKey(ASYNCHRONOUS)
    //                                || annotations.containsKey(BULKHEAD)
    //                                || annotations.containsKey(CIRCUIT_BREAKER)
    //                                || annotations.containsKey(FALLBACK)
    //                                || annotations.containsKey(RETRY)
    //                                || annotations.containsKey(TIMEOUT)) {
    //                            ctx.transform().add(ActivateRequestContext.class).done();
    //                            break;
    //                        }
    //                        clazz = index.getIndex().getClassByName(clazz.superName());
    //                    }
    //                } else if (ctx.isMethod()) {
    //                    MethodInfo method = ctx.getTarget().asMethod();
    //                    if (method.hasAnnotation(ASYNCHRONOUS)
    //                            || method.hasAnnotation(BULKHEAD)
    //                            || method.hasAnnotation(CIRCUIT_BREAKER)
    //                            || method.hasAnnotation(FALLBACK)
    //                            || method.hasAnnotation(RETRY)
    //                            || method.hasAnnotation(TIMEOUT)) {
    //                        ctx.transform().add(ActivateRequestContext.class).done();
    //                    }
    //                }
    //            }
    //        });
    //    }

    @BuildStep
    public ConfigurationTypeBuildItem registerTypes() {
        return new ConfigurationTypeBuildItem(ChronoUnit.class);
    }

    @BuildStep
    public void logCleanup(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilter) {
        logCleanupFilter.produce(new LogCleanupFilterBuildItem("io.smallrye.faulttolerance.HystrixInitializer",
                "### Init Hystrix ###",
                "### Reset Hystrix ###",
                // no need to log the strategy if it is the default
                "Hystrix concurrency strategy used: DefaultHystrixConcurrencyStrategy"));
        logCleanupFilter.produce(new LogCleanupFilterBuildItem("io.smallrye.faulttolerance.DefaultHystrixConcurrencyStrategy",
                "### Privilleged Thread Factory used ###"));

        logCleanupFilter.produce(new LogCleanupFilterBuildItem("com.netflix.config.sources.URLConfigurationSource",
                "No URLs will be polled as dynamic configuration sources.",
                "To enable URLs as dynamic configuration sources"));
        logCleanupFilter.produce(new LogCleanupFilterBuildItem("com.netflix.config.DynamicPropertyFactory",
                "DynamicPropertyFactory is initialized with configuration sources"));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public void clearStatic(SmallryeFaultToleranceRecorder recorder, ShutdownContextBuildItem context,
            BeanContainerBuildItem beanContainer) {
        // impl note - we depend on BeanContainerBuildItem to make sure Arc registers before SR FT
        // this is needed so that shutdown context of FT is executed before Arc container shuts down
        recorder.resetCommandContextOnUndeploy(context);
    }
}
