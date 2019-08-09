
package io.quarkus.smallrye.metrics.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.GAUGE;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.METRICS_ANNOTATIONS;
import static io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames.METRICS_BINDING;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
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
import io.smallrye.metrics.interceptors.MetricsInterceptor;
import io.smallrye.metrics.interceptors.TimedInterceptor;

public class SmallRyeMetricsProcessor {
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
                ClassInfo clazz = ctx.getTarget().asClass();
                if (clazz.name().toString()
                        .startsWith(io.smallrye.metrics.interceptors.MetricsInterceptor.class.getPackage().getName())) {
                    return;
                }

                for (DotName annotationName : clazz.annotations().keySet()) {
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
        return new AutoInjectAnnotationBuildItem(SmallRyeMetricsDotNames.METRIC);
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

        for (DotName metricsAnnotation : METRICS_ANNOTATIONS) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, metricsAnnotation.toString()));
        }

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, METRICS_BINDING.toString()));
        metrics.createRegistries(beanContainerBuildItem.getValue());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
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
        IndexView index = beanArchiveIndex.getIndex();
        JandexBeanInfoAdapter beanInfoAdapter = new JandexBeanInfoAdapter(index);
        JandexMemberInfoAdapter memberInfoAdapter = new JandexMemberInfoAdapter(index);

        Set<MethodInfo> collectedMetricsMethods = new HashSet<>();
        Map<DotName, ClassInfo> collectedMetricsClasses = new HashMap<>();

        for (DotName metricAnnotation : METRICS_ANNOTATIONS) {
            Collection<AnnotationInstance> metricAnnotationInstances = index.getAnnotations(metricAnnotation);
            for (AnnotationInstance metricAnnotationInstance : metricAnnotationInstances) {
                AnnotationTarget metricAnnotationTarget = metricAnnotationInstance.target();
                switch (metricAnnotationTarget.kind()) {
                    case METHOD: {
                        MethodInfo method = metricAnnotationTarget.asMethod();
                        if (!method.declaringClass().name().toString().startsWith("io.smallrye.metrics")) {
                            collectedMetricsMethods.add(method);
                        }
                        break;
                    }
                    case CLASS: {
                        ClassInfo clazz = metricAnnotationTarget.asClass();
                        if (!clazz.name().toString().startsWith("io.smallrye.metrics")) {
                            collectMetricsClassAndSubClasses(index, collectedMetricsClasses, clazz);
                        }
                        break;
                    }
                }
            }
        }

        for (ClassInfo clazz : collectedMetricsClasses.values()) {
            BeanInfo beanInfo = beanInfoAdapter.convert(clazz);
            for (MethodInfo method : clazz.methods()) {
                metrics.registerMetrics(beanInfo, memberInfoAdapter.convert(method));
            }
        }

        for (MethodInfo method : collectedMetricsMethods) {
            ClassInfo declaringClazz = method.declaringClass();
            if (!collectedMetricsClasses.containsKey(declaringClazz.name())) {
                BeanInfo beanInfo = beanInfoAdapter.convert(declaringClazz);
                metrics.registerMetrics(beanInfo, memberInfoAdapter.convert(method));
            }
        }
    }

    private void collectMetricsClassAndSubClasses(IndexView index, Map<DotName, ClassInfo> collectedMetricsClasses,
            ClassInfo clazz) {
        collectedMetricsClasses.put(clazz.name(), clazz);
        for (ClassInfo subClass : index.getAllKnownSubclasses(clazz.name())) {
            collectedMetricsClasses.put(subClass.name(), subClass);
        }
    }

    //    @BuildStep
    //    @Record(STATIC_INIT)
    void registerMetricsFromProducerFieldsAndMethods(BeanArchiveIndexBuildItem beanArchiveIndex) {
        // TODO: Is this possible at all? to register such metric we need to actually instantiate the object produced by the field/method, which we can't do in STATIC_INIT?!
    }

}
