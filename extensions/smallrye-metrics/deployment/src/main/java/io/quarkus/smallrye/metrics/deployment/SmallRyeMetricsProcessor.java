package io.quarkus.smallrye.metrics.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.metrics.deployment.jandex.JandexBeanInfoAdapter;
import io.quarkus.smallrye.metrics.deployment.jandex.JandexMemberInfoAdapter;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsRecorder;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsServlet;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.smallrye.metrics.MetricProducer;
import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.MetricsRequestHandler;
import io.smallrye.metrics.elementdesc.BeanInfo;
import io.smallrye.metrics.interceptors.ConcurrentGaugeInterceptor;
import io.smallrye.metrics.interceptors.CountedInterceptor;
import io.smallrye.metrics.interceptors.MeteredInterceptor;
import io.smallrye.metrics.interceptors.MetricNameFactory;
import io.smallrye.metrics.interceptors.MetricsBinding;
import io.smallrye.metrics.interceptors.MetricsInterceptor;
import io.smallrye.metrics.interceptors.TimedInterceptor;

public class SmallRyeMetricsProcessor {

    public static final DotName GAUGE = DotName.createSimple(Gauge.class.getName());
    public static final DotName COUNTED = DotName.createSimple(Counted.class.getName());
    public static final DotName TIMED = DotName.createSimple(Timed.class.getName());
    public static final DotName METERED = DotName.createSimple(Metered.class.getName());
    public static final DotName CONCURRENT_GAUGE = DotName.createSimple(ConcurrentGauge.class.getName());
    public static final DotName METRICS_BINDING = DotName.createSimple(MetricsBinding.class.getName());

    public static final Set<DotName> metricsAnnotations = new HashSet<>(Arrays.asList(
            GAUGE,
            COUNTED,
            TIMED,
            METERED,
            CONCURRENT_GAUGE));

    @ConfigRoot(name = "smallrye-metrics")
    static final class SmallRyeMetricsConfig {

        /**
         * The path to the metrics Servlet.
         */
        @ConfigItem(defaultValue = "/metrics")
        String path;
    }

    SmallRyeMetricsConfig metrics;

    @BuildStep
    ServletBuildItem createServlet() {
        ServletBuildItem servletBuildItem = ServletBuildItem.builder("metrics", SmallRyeMetricsServlet.class.getName())
                .addMapping(metrics.path + (metrics.path.endsWith("/") ? "*" : "/*"))
                .build();
        return servletBuildItem;
    }

    @BuildStep
    void beans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(MetricProducer.class,
                MetricNameFactory.class,
                MetricRegistries.class,
                MetricsInterceptor.class,
                MeteredInterceptor.class,
                ConcurrentGaugeInterceptor.class,
                CountedInterceptor.class,
                TimedInterceptor.class,
                MetricsRequestHandler.class,
                SmallRyeMetricsServlet.class));
    }

    @BuildStep
    void annotationTransformers(BuildProducer<AnnotationsTransformerBuildItem> transformers) {
        // attach @MetricsBinding to each class that contains any metric annotations
        transformers.produce(new AnnotationsTransformerBuildItem(ctx -> {
            if (ctx.isClass()) {
                // skip classes in package io.smallrye.metrics.interceptors
                if (ctx.getTarget().asClass().name().toString()
                        .startsWith(io.smallrye.metrics.interceptors.MetricsInterceptor.class.getPackage().getName())) {
                    return;
                }
                for (DotName annotationName : ctx.getTarget().asClass().annotations().keySet()) {
                    if (GAUGE.equals(annotationName)) {
                        ctx.transform().add(AnnotationInstance.create(METRICS_BINDING,
                                ctx.getTarget(), new AnnotationValue[0]))
                                .done();
                        return;
                    }
                }
            }
        }));

    }

    @BuildStep
    AutoInjectAnnotationBuildItem autoInjectMetric() {
        return new AutoInjectAnnotationBuildItem(DotName.createSimple(Metric.class.getName()));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(BeanContainerBuildItem beanContainerBuildItem,
            SmallRyeMetricsRecorder metrics,
            ShutdownContextBuildItem shutdown,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<FeatureBuildItem> feature) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_METRICS));

        for (DotName metricsAnnotation : metricsAnnotations) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, metricsAnnotation.toString()));
        }
        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, MetricsBinding.class.getName()));

        metrics.createRegistries(beanContainerBuildItem.getValue());
    }

    @BuildStep
    @Record(STATIC_INIT)
    void registerBaseAndVendorMetrics(SmallRyeMetricsRecorder metrics, ShutdownContextBuildItem shutdown) {
        metrics.registerBaseMetrics(shutdown);
        metrics.registerVendorMetrics(shutdown);
    }

    @BuildStep
    public void logCleanup(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilter) {
        logCleanupFilter.produce(new LogCleanupFilterBuildItem("io.smallrye.metrics.MetricsRegistryImpl",
                "Register metric ["));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void registerMetricsFromAnnotatedMethods(SmallRyeMetricsRecorder metrics,
            BeanContainerBuildItem beanContainerBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndex) {
        JandexBeanInfoAdapter beanInfoAdapter = new JandexBeanInfoAdapter(beanArchiveIndex.getIndex());
        JandexMemberInfoAdapter memberInfoAdapter = new JandexMemberInfoAdapter(beanArchiveIndex.getIndex());
        for (ClassInfo clazz : beanArchiveIndex.getIndex().getKnownClasses()) {
            // TODO: maybe leave out more cases where we can be sure that there are no metrics to be registered
            // to speed up the startup
            if (clazz.name().prefix().toString().startsWith("io.smallrye.metrics")) {
                continue;
            }
            BeanInfo beanInfo = beanInfoAdapter.convert(clazz);
            for (MethodInfo method : clazz.methods()) {
                if (containsMetricsAnnotations(method.annotations()) ||
                        containsMetricsAnnotations(clazz.classAnnotations())) {
                    metrics.registerMetrics(beanInfo, memberInfoAdapter.convert(method));
                }
            }
        }
    }

    private boolean containsMetricsAnnotations(Collection<AnnotationInstance> annotations) {
        return annotations.stream().anyMatch(annotation -> metricsAnnotations.contains(annotation.name()));
    }

    //    @BuildStep
    //    @Record(STATIC_INIT)
    void registerMetricsFromProducerFieldsAndMethods(BeanArchiveIndexBuildItem beanArchiveIndex) {
        // TODO: Is this possible at all? to register such metric we need to actually instantiate the object produced by the field/method, which we can't do in STATIC_INIT?!
    }

}
