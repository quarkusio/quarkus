package io.quarkus.opentelemetry.deployment.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * MicroProfile Telemetry requires the {@code otel.*} configuration properties to be honored. With
 * {@code quarkus.otel.mp.compatibility} enabled, {@code otel.propagators} must determine the propagator list, as
 * the MicroProfile Telemetry TCK's {@code W3PropagationTest} and {@code PropagatorSpiTest} rely on. Setting it to
 * {@code tracecontext} only must leave the W3C baggage propagator out.
 */
public class OpenTelemetryMpCompatibilityPropagatorsTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(ManifestContainer::addManifest)
            .overrideConfigKey("quarkus.otel.mp.compatibility", "true")
            .overrideConfigKey("otel.propagators", "tracecontext")
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void otelPropagatorsPropertyIsHonored() {
        Collection<String> fields = openTelemetry.getPropagators().getTextMapPropagator().fields();
        assertThat(fields)
                .contains("traceparent")
                .doesNotContain("baggage");
    }
}
