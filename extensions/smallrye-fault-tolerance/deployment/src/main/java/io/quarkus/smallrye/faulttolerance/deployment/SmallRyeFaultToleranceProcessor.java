package io.quarkus.smallrye.faulttolerance.deployment;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.netflix.hystrix.HystrixCircuitBreaker;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.QuarkusConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFallbackHandlerProvider;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFaultToleranceOperationProvider;
import io.quarkus.smallrye.faulttolerance.runtime.SmallryeFaultToleranceRecorder;
import io.smallrye.faulttolerance.DefaultCommandListenersProvider;
import io.smallrye.faulttolerance.DefaultHystrixConcurrencyStrategy;
import io.smallrye.faulttolerance.HystrixCommandBinding;
import io.smallrye.faulttolerance.HystrixCommandInterceptor;
import io.smallrye.faulttolerance.HystrixInitializer;
import io.smallrye.faulttolerance.metrics.MetricsCollectorFactory;

public class SmallRyeFaultToleranceProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<NativeImageSystemPropertyBuildItem> nativeImageSystemProperty;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    NativeImageSystemPropertyBuildItem disableJmx() {
        return new NativeImageSystemPropertyBuildItem("archaius.dynamicPropertyFactory.registerConfigWithJMX", "false");
    }

    @BuildStep
    public void build(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            BuildProducer<FeatureBuildItem> feature, BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> additionalBda,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperty) throws Exception {

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
        nativeImageSystemProperty.produce(new NativeImageSystemPropertyBuildItem("rx.unsafe-disable", "true"));

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
                MetricsCollectorFactory.class);
        additionalBean.produce(builder.build());

        if (!capabilities.isCapabilityPresent(Capabilities.METRICS)) {
            //disable fault tolerance metrics with the MP sys props and provides a No-op metric registry.
            additionalBean.produce(new AdditionalBeanBuildItem(NoopMetricRegistry.class));
            systemProperty.produce(new SystemPropertyBuildItem("MP_Fault_Tolerance_Metrics_Enabled", "false"));
        }
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformInterceptorPriority(BeanArchiveIndexBuildItem index) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (ctx.isClass()) {
                    if (!ctx.getTarget().asClass().name().toString()
                            .equals("io.smallrye.faulttolerance.HystrixCommandInterceptor")) {
                        return;
                    }

                    Integer priority = QuarkusConfig.getBoxedInt("mp.fault.tolerance.interceptor.priority", null, true);
                    if (priority != null) {
                        ctx.transform()
                                .remove(ann -> ann.name().toString().equals(Priority.class.getName()))
                                .add(Priority.class, AnnotationValue.createIntegerValue("value", priority))
                                .done();
                    }
                }
            }
        });
    }

    @BuildStep
    // needs to be RUNTIME_INIT because we need to read MP Config
    @Record(ExecutionTime.RUNTIME_INIT)
    void validateFaultToleranceAnnotations(
            ValidationPhaseBuildItem validationPhase, SmallryeFaultToleranceRecorder recorder) {
        List<String> beanNames = new ArrayList<>();
        for (BeanInfo bean : validationPhase.getContext().get(BuildExtension.Key.BEANS)) {
            if (bean.isClassBean()) {
                beanNames.add(bean.getBeanClass().toString());
            }
        }
        recorder.validate(beanNames);
    }

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
