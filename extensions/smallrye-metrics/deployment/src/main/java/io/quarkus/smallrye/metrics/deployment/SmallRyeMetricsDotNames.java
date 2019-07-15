package io.quarkus.smallrye.metrics.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.smallrye.metrics.interceptors.MetricsBinding;

public class SmallRyeMetricsDotNames {
    public static final DotName GAUGE = DotName.createSimple(Gauge.class.getName());
    public static final DotName TIMED = DotName.createSimple(Timed.class.getName());
    public static final DotName METRIC = DotName.createSimple(Metric.class.getName());
    public static final DotName COUNTED = DotName.createSimple(Counted.class.getName());
    public static final DotName METERED = DotName.createSimple(Metered.class.getName());
    public static final DotName METRICS_BINDING = DotName.createSimple(MetricsBinding.class.getName());
    public static final DotName CONCURRENT_GAUGE = DotName.createSimple(ConcurrentGauge.class.getName());

    public static final Set<DotName> METRICS_ANNOTATIONS = new HashSet<>(Arrays.asList(
            GAUGE,
            TIMED,
            COUNTED,
            METERED,
            CONCURRENT_GAUGE));

    public static boolean isMetricAnnotation(AnnotationInstance instance) {
        return METRICS_ANNOTATIONS.contains(instance.name());
    }
}
