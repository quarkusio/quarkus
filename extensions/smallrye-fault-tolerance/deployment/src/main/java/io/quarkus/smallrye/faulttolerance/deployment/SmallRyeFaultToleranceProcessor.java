package io.quarkus.smallrye.faulttolerance.deployment;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
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
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.smallrye.faulttolerance.runtime.NoopMetricRegistry;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFallbackHandlerProvider;
import io.quarkus.smallrye.faulttolerance.runtime.QuarkusFaultToleranceOperationProvider;
import io.quarkus.smallrye.faulttolerance.runtime.SmallryeFaultToleranceRecorder;
import io.smallrye.faulttolerance.ExecutorFactory;
import io.smallrye.faulttolerance.ExecutorProvider;
import io.smallrye.faulttolerance.FaultToleranceBinding;
import io.smallrye.faulttolerance.FaultToleranceInterceptor;
import io.smallrye.faulttolerance.internal.StrategyCache;
import io.smallrye.faulttolerance.metrics.MetricsCollectorFactory;
import io.smallrye.faulttolerance.propagation.ContextPropagationExecutorFactory;

public class SmallRyeFaultToleranceProcessor {

    private static final Set<DotName> FT_ANNOTATIONS = new HashSet<>();
    static {
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
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageSystemPropertyBuildItem> nativeImageSystemProperty) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_FAULT_TOLERANCE));

        serviceProvider.produce(new ServiceProviderBuildItem(ExecutorFactory.class.getName(),
                ContextPropagationExecutorFactory.class.getName()));

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
                ExecutorProvider.class,
                StrategyCache.class,
                QuarkusFaultToleranceOperationProvider.class,
                QuarkusFallbackHandlerProvider.class,
                MetricsCollectorFactory.class);
        additionalBean.produce(builder.build());

        if (!capabilities.isCapabilityPresent(Capabilities.METRICS)) {
            //disable fault tolerance metrics with the MP sys props and provides a No-op metric registry.
            additionalBean.produce(AdditionalBeanBuildItem.builder().addBeanClass(NoopMetricRegistry.class).setRemovable()
                    .setDefaultScope(DotName.createSimple(Singleton.class.getName())).build());
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
    void validateFaultToleranceAnnotations(SmallryeFaultToleranceRecorder recorder,
            ValidationPhaseBuildItem validationPhase,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {
        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        Set<String> beanNames = new HashSet<>();
        IndexView index = beanArchiveIndexBuildItem.getIndex();

        for (BeanInfo info : validationPhase.getContext().beans()) {
            if (hasFTAnnotations(index, annotationStore, info.getImplClazz())) {
                beanNames.add(info.getBeanClass().toString());
            }
        }

        recorder.createFaultToleranceOperation(beanNames);
    }

    private boolean hasFTAnnotations(IndexView index, AnnotationStore annotationStore, ClassInfo info) {
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
        ClassInfo parentClassInfo = index.getClassByName(parentClassName);
        if (parentClassName.equals(DotNames.OBJECT) || parentClassInfo == null) {
            return false; //no more parents
        }

        return hasFTAnnotations(index, annotationStore, parentClassInfo);
    }

    @BuildStep
    public ConfigurationTypeBuildItem registerTypes() {
        return new ConfigurationTypeBuildItem(ChronoUnit.class);
    }
}
