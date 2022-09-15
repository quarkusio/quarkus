package io.quarkus.smallrye.metrics.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.smallrye.metrics.interceptors.MetricsBinding;

public class SmallRyeMetricsDotNames {

    // metric interfaces
    public static final DotName METRIC_INTERFACE = DotName
            .createSimple(org.eclipse.microprofile.metrics.Metric.class.getName());
    public static final DotName GAUGE_INTERFACE = DotName
            .createSimple(org.eclipse.microprofile.metrics.Gauge.class.getName());
    public static final DotName COUNTER_INTERFACE = DotName
            .createSimple(Counter.class.getName());
    public static final DotName CONCURRENT_GAUGE_INTERFACE = DotName
            .createSimple(org.eclipse.microprofile.metrics.ConcurrentGauge.class.getName());
    public static final DotName METER_INTERFACE = DotName
            .createSimple(Meter.class.getName());
    public static final DotName SIMPLE_TIMER_INTERFACE = DotName
            .createSimple(SimpleTimer.class.getName());
    public static final DotName TIMER_INTERFACE = DotName
            .createSimple(Timer.class.getName());
    public static final DotName HISTOGRAM_INTERFACE = DotName
            .createSimple(Histogram.class.getName());

    // annotations
    public static final DotName GAUGE = DotName.createSimple(Gauge.class.getName());
    public static final DotName TIMED = DotName.createSimple(Timed.class.getName());
    public static final DotName SIMPLY_TIMED = DotName.createSimple(SimplyTimed.class.getName());
    public static final DotName METRIC = DotName.createSimple(Metric.class.getName());
    public static final DotName COUNTED = DotName.createSimple(Counted.class.getName());
    public static final DotName METERED = DotName.createSimple(Metered.class.getName());
    public static final DotName METRICS_BINDING = DotName.createSimple(MetricsBinding.class.getName());
    public static final DotName CONCURRENT_GAUGE = DotName.createSimple(ConcurrentGauge.class.getName());

    public static final Set<DotName> METRICS_ANNOTATIONS = new HashSet<>(Arrays.asList(
            GAUGE,
            TIMED,
            SIMPLY_TIMED,
            COUNTED,
            METERED,
            CONCURRENT_GAUGE));

    public static boolean isMetricAnnotation(AnnotationInstance instance) {
        return METRICS_ANNOTATIONS.contains(instance.name());
    }

    // these are needed for determining whether a class is a REST endpoint or JAX-RS provider
    public static final DotName JAXRS_PATH = DotName.createSimple("jakarta.ws.rs.Path");
    public static final DotName REST_CONTROLLER = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");
    public static final DotName JAXRS_PROVIDER = DotName.createSimple("jakarta.ws.rs.ext.Provider");

}
