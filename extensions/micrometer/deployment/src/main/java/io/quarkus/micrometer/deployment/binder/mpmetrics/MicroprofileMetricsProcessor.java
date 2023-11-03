package io.quarkus.micrometer.deployment.binder.mpmetrics;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import jakarta.enterprise.context.Dependent;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.micrometer.deployment.RootMeterRegistryBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.binder.mpmetrics.MpMetricsRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

/**
 * The microprofile API must remain optional.
 *
 * Avoid importing classes that import MP Metrics API classes.
 */
@BuildSteps(onlyIf = MicroprofileMetricsProcessor.MicroprofileMetricsEnabled.class)
public class MicroprofileMetricsProcessor {
    private static final Logger log = Logger.getLogger(MicroprofileMetricsProcessor.class);
    static final Class<?> METRIC_ANNOTATION_CLASS = MicrometerRecorder
            .getClassForName(MetricDotNames.METRIC_ANNOTATION.toString());

    static class MicroprofileMetricsEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return METRIC_ANNOTATION_CLASS != null && mConfig.checkBinderEnabledWithDefault(mConfig.binder.mpMetrics);
        }
    }

    @BuildStep
    IndexDependencyBuildItem addDependencies() {
        return new IndexDependencyBuildItem("org.eclipse.microprofile.metrics", "microprofile-metrics-api");
    }

    @BuildStep
    AutoInjectAnnotationBuildItem autoInjectMetric() {
        return new AutoInjectAnnotationBuildItem(MetricDotNames.METRIC);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBeanClasses() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(MetricDotNames.MP_METRICS_BINDER.toString())
                .addBeanClass(MetricDotNames.CONCURRENT_GAUGE_INTERCEPTOR.toString())
                .addBeanClass(MetricDotNames.COUNTED_INTERCEPTOR.toString())
                .addBeanClass(MetricDotNames.INJECTED_METRIC_PRODUCER.toString())
                .addBeanClass(MetricDotNames.TIMED_INTERCEPTOR.toString())
                .addBeanClass(MetricDotNames.MP_METRICS_REGISTRY_PRODUCER.toString())
                .build();
    }

    @BuildStep
    void logWarningForMpMetricsUsage(CombinedIndexBuildItem combinedIndexBuildItem,
            BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<BeanConfiguratorBuildItem> errors) {
        IndexView index = combinedIndexBuildItem.getIndex();
        boolean mpMetricsPresent = false;

        // Find usage of MP Metrics annotations
        for (DotName annotation : MetricDotNames.individualMetrics) {
            if (index.getAnnotations(annotation).size() > 0) {
                mpMetricsPresent = true;
                break;
            }
        }

        if (!mpMetricsPresent) {
            if (index.getAnnotations(MetricDotNames.METRIC_ANNOTATION).size() > 0) {
                mpMetricsPresent = true;
            }
        }

        if (!mpMetricsPresent) {
            for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getInjectionPoints()) {
                if (injectionPoint.getRequiredType().name().equals(MetricDotNames.METRIC_REGISTRY)) {
                    mpMetricsPresent = true;
                    break;
                }
            }
        }

        if (mpMetricsPresent) {
            log.warn("This application uses the MP Metrics API. " +
                    "The micrometer extension currently provides a compatibility layer that supports the MP Metrics API, " +
                    "but metric names and recorded values will be different. " +
                    "Note that the MP Metrics compatibility layer will move to a different extension in the future.");
        }
    }

    /**
     * Make sure all classes containing metrics annotations have a bean scope.
     */
    @BuildStep
    AnnotationsTransformerBuildItem transformBeanScope(BeanArchiveIndexBuildItem index,
            CustomScopeAnnotationsBuildItem scopes) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public int getPriority() {
                // this specifically should run after the JAX-RS AnnotationTransformers
                return BuildExtension.DEFAULT_PRIORITY - 100;
            }

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (scopes.isScopeIn(ctx.getAnnotations())) {
                    return;
                }
                ClassInfo clazz = ctx.getTarget().asClass();
                if (!MetricDotNames.isSingleInstance(clazz)) {
                    while (clazz != null && clazz.superName() != null) {
                        if (!MetricDotNames.knownClass(clazz)
                                && MetricDotNames.containsMetricAnnotation(clazz.annotationsMap())) {
                            log.debugf(
                                    "Found metrics business methods on a class %s with no scope defined - adding @Dependent",
                                    ctx.getTarget());
                            ctx.transform().add(Dependent.class).done();
                            break;
                        }
                        clazz = index.getIndex().getClassByName(clazz.superName());
                    }
                }
            }
        });
    }

    @BuildStep
    UnremovableBeanBuildItem processAnnotatedMetrics(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformers,
            CombinedIndexBuildItem indexBuildItem) {
        IndexView index = indexBuildItem.getIndex();
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

        // Gauges.
        GaugeAnnotationHandler.processAnnotatedGauges(index, classOutput);
        annotationsTransformers.produce(AnnotationHandler.transformAnnotations(index,
                MetricDotNames.CONCURRENT_GAUGE_ANNOTATION));

        // Invocation counters
        annotationsTransformers.produce(AnnotationHandler.transformAnnotations(index,
                MetricDotNames.COUNTED_ANNOTATION));
        annotationsTransformers.produce(AnnotationHandler.transformAnnotations(index,
                MetricDotNames.METERED_ANNOTATION, MetricDotNames.COUNTED_ANNOTATION));

        // Timed annotations. SimplyTimed --> Timed
        annotationsTransformers.produce(AnnotationHandler.transformAnnotations(index,
                MetricDotNames.TIMED_ANNOTATION));
        annotationsTransformers.produce(AnnotationHandler.transformAnnotations(index,
                MetricDotNames.SIMPLY_TIMED_ANNOTATION, MetricDotNames.TIMED_ANNOTATION));

        // @Metric
        annotationsTransformers.produce(AnnotationHandler.transformAnnotations(index,
                MetricDotNames.METRIC_ANNOTATION));

        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(io.quarkus.arc.processor.BeanInfo beanInfo) {
                if (beanInfo.hasType(MetricDotNames.METRIC) ||
                        beanInfo.hasType(MetricDotNames.ANNOTATED_GAUGE_ADAPTER)) {
                    return true;
                }
                return false;
            }
        });
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureRegistry(MpMetricsRecorder recorder,
            RootMeterRegistryBuildItem rootMeterRegistryBuildItem) {
        recorder.configureRegistryAdapter(rootMeterRegistryBuildItem.getValue());

    }
}
