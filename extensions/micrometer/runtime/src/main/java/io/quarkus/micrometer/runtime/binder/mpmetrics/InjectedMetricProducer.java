package io.quarkus.micrometer.runtime.binder.mpmetrics;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.jboss.logging.Logger;

/**
 * Create default producer methods for {literal @}Inject {literal @}Metric
 * annotations requiring {@code Meter}, {@code Timer}, {@code Counter},
 * and {@code Histogram}.
 *
 * Due to build-time processing, {literal @}Metric annotations always have
 * a name value that has been resolved according to MP Metrics naming conventions.
 */
@SuppressWarnings("unused")
@Singleton
class InjectedMetricProducer {
    private static final Logger log = Logger.getLogger(InjectedMetricProducer.class);

    // Micrometer meter registry
    final MetricRegistryAdapter mpRegistry;

    InjectedMetricProducer(MetricRegistryAdapter mpRegistry) {
        this.mpRegistry = mpRegistry;
    }

    @Produces
    Counter getCounter(InjectionPoint ip) {
        Metric metricInfo = ip.getAnnotated().getAnnotation(Metric.class);
        return mpRegistry.injectedCounter(metricInfo);
    }

    /**
     * For a programmatic concurrent gauge, create a gauge around
     * a simple implementation that uses a {@code LongAdder}.
     * The metrics gathered this way will not be as rich as with the
     * {@code LongTimerTask}-based metrics used with the
     * {literal @}ConcurrentGauge annotation, but is the best the API
     * semantics allow (decrement/increment).
     */
    @Produces
    ConcurrentGauge getConcurrentGauge(InjectionPoint ip) {
        Metric metricInfo = ip.getAnnotated().getAnnotation(Metric.class);
        return mpRegistry.injectedConcurrentGauge(metricInfo);
    }

    @Produces
    Histogram getHistogram(InjectionPoint ip) {
        Metric metricInfo = ip.getAnnotated().getAnnotation(Metric.class);
        return mpRegistry.injectedHistogram(metricInfo);
    }

    @Produces
    Meter getMeter(InjectionPoint ip) {
        Metric metricInfo = ip.getAnnotated().getAnnotation(Metric.class);
        return mpRegistry.injectedMeter(metricInfo);
    }

    @Produces
    SimpleTimer getSimpleTimer(InjectionPoint ip) {
        Metric metricInfo = ip.getAnnotated().getAnnotation(Metric.class);
        return mpRegistry.injectedSimpleTimer(metricInfo);
    }

    @Produces
    Timer getTimer(InjectionPoint ip) {
        Metric metricInfo = ip.getAnnotated().getAnnotation(Metric.class);
        return mpRegistry.injectedTimer(metricInfo);
    }
}
