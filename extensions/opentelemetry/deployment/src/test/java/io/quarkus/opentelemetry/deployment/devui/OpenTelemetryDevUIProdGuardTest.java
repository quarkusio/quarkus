package io.quarkus.opentelemetry.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.runtime.devui.DevUiTracesSpanProcessor;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Guards the design's "not a bean outside dev": in a non-dev build the capture
 * processor must not be in the active tracer pipeline, even though its class ships in
 * the runtime jar. Directly catches accidental ArC auto-discovery.
 */
public class OpenTelemetryDevUIProdGuardTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((JavaArchive jar) -> jar.addClass(TestUtil.class))
            // Keep the test hermetic: no OTLP export and no LGTM/observability dev
            // service container (which would also override the OTLP exporter).
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none")
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    public void devUiProcessorIsNotInThePipeline() throws Exception {
        List<SpanProcessor> processors = TestUtil.getSpanProcessors(openTelemetry);
        assertThat(processors).noneMatch(DevUiTracesSpanProcessor.class::isInstance);
    }
}
