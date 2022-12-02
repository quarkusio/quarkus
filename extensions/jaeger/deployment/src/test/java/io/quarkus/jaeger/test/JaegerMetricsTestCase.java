package io.quarkus.jaeger.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JaegerMetricsTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application-metrics-enabled.properties");

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry registry;

    /**
     * We're not running a Jaeger instance to be able to test anything thoroughly,
     * so just check that the metrics are registered after start.
     */
    @Test
    public void test() {
        Set<String> registeredMetrics = registry.getMetrics().keySet().stream().map(MetricID::getName)
                .collect(Collectors.toSet());
        assertTrue(registeredMetrics.contains("jaeger_tracer_baggage_restrictions_updates"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_baggage_updates"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_baggage_truncations"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_finished_spans"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_reporter_queue_length"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_reporter_spans"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_sampler_queries"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_sampler_updates"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_span_context_decoding_errors"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_started_spans"));
        assertTrue(registeredMetrics.contains("jaeger_tracer_traces"));
    }

}
