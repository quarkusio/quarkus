package io.quarkus.micrometer.deployment.binder.mpmetrics;

import java.util.*;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BuiltinScope;

/**
 * The microprofile API must remain optional.
 *
 * Avoid importing classes that import MP Metrics API classes.
 */
public class MetricDotNames {
    static final String MICROMETER_EXTENSION_PKG = "io.quarkus.micrometer.runtime.binder.mpmetrics";

    // Use string class names: do not force-load a class that pulls in microprofile dependencies
    static final DotName MP_METRICS_BINDER = DotName
            .createSimple("io.quarkus.micrometer.runtime.binder.mpmetrics.MpMetricsBinder");
    static final DotName CONCURRENT_GAUGE_ANNOTATION = DotName
            .createSimple("org.eclipse.microprofile.metrics.annotation.ConcurrentGauge");
    static final DotName COUNTED_ANNOTATION = DotName.createSimple("org.eclipse.microprofile.metrics.annotation.Counted");
    static final DotName GAUGE_ANNOTATION = DotName.createSimple("org.eclipse.microprofile.metrics.annotation.Gauge");
    static final DotName METERED_ANNOTATION = DotName.createSimple("org.eclipse.microprofile.metrics.annotation.Metered");
    static final DotName SIMPLY_TIMED_ANNOTATION = DotName
            .createSimple("org.eclipse.microprofile.metrics.annotation.SimplyTimed");
    static final DotName TIMED_ANNOTATION = DotName.createSimple("org.eclipse.microprofile.metrics.annotation.Timed");

    static final Set<DotName> individualMetrics = new HashSet<>(Arrays.asList(
            CONCURRENT_GAUGE_ANNOTATION,
            COUNTED_ANNOTATION,
            GAUGE_ANNOTATION,
            METERED_ANNOTATION,
            SIMPLY_TIMED_ANNOTATION,
            TIMED_ANNOTATION));

    static final DotName METRIC_REGISTRY = DotName.createSimple("org.eclipse.microprofile.metrics.MetricRegistry");

    static final DotName METRIC_ANNOTATION = DotName
            .createSimple("org.eclipse.microprofile.metrics.annotation.Metric");
    static final DotName ANNOTATED_GAUGE_ADAPTER = DotName
            .createSimple("io.quarkus.micrometer.runtime.binder.mpmetrics.AnnotatedGaugeAdapter");

    static final DotName METRIC = DotName
            .createSimple("org.eclipse.microprofile.metrics.Metric");

    // these are needed for determining whether a class is a REST endpoint or JAX-RS provider
    static final DotName JAXRS_PATH = DotName.createSimple("jakarta.ws.rs.Path");
    static final DotName REST_CONTROLLER = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");

    // Interceptors and producers
    static final DotName CONCURRENT_GAUGE_INTERCEPTOR = DotName
            .createSimple("io.quarkus.micrometer.runtime.binder.mpmetrics.ConcurrentGaugeInterceptor");
    static final DotName COUNTED_INTERCEPTOR = DotName
            .createSimple("io.quarkus.micrometer.runtime.binder.mpmetrics.CountedInterceptor");
    static final DotName INJECTED_METRIC_PRODUCER = DotName
            .createSimple("io.quarkus.micrometer.runtime.binder.mpmetrics.InjectedMetricProducer");
    static final DotName TIMED_INTERCEPTOR = DotName
            .createSimple("io.quarkus.micrometer.runtime.binder.mpmetrics.TimedInterceptor");
    static final DotName MP_METRICS_REGISTRY_PRODUCER = DotName
            .createSimple("io.quarkus.micrometer.runtime.binder.mpmetrics.MpMetricsRegistryProducer");

    /**
     * @param annotations
     * @return true if the map of all annotations contains any MP Metrics
     *         annotations
     */
    static boolean containsMetricAnnotation(Map<DotName, List<AnnotationInstance>> annotations) {
        for (DotName name : individualMetrics) {
            if (annotations.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true for known metrics subsystem classes that should not
     *         be inspected for lifecycle constraints, etc.
     */
    static boolean knownClass(ClassInfo classInfo) {
        return classInfo.name().toString().startsWith(MICROMETER_EXTENSION_PKG);
    }

    /**
     * @param classInfo
     * @return true if the specified class is either a REST endpoint or
     *         has a singleton/application scope.
     */
    static boolean isSingleInstance(ClassInfo classInfo) {
        BuiltinScope beanScope = BuiltinScope.from(classInfo);
        return classInfo.annotationsMap().containsKey(REST_CONTROLLER) ||
                classInfo.annotationsMap().containsKey(JAXRS_PATH) ||
                BuiltinScope.APPLICATION.equals(beanScope) ||
                BuiltinScope.SINGLETON.equals(beanScope);
    }
}
