package io.quarkus.opentelemetry.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIBuildTimeDataTest;
import io.quarkus.test.QuarkusDevModeTest;
import tools.jackson.databind.JsonNode;

/**
 * Verifies that the OpenTelemetry extension contributes a "Traces" signal to the
 * standalone core "Observability" Dev UI section. The signals are exposed as the
 * {@code observabilitySignals} build-time data under the core {@code devui} namespace.
 */
public class ObservabilitySectionDevUITest extends DevUIBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addAsResource(new StringAsset(
                            "quarkus.otel.traces.exporter=none\n"
                                    + "quarkus.otel.metrics.exporter=none\n"
                                    + "quarkus.otel.logs.exporter=none\n"
                                    + "quarkus.devservices.enabled=false\n"),
                            "application.properties"));

    public ObservabilitySectionDevUITest() {
        super("devui");
    }

    @Test
    public void contributesTracesSignalToObservabilitySection() throws Exception {
        JsonNode signals = super.getBuildTimeData("observabilitySignals");
        assertThat(signals).isNotNull();
        assertThat(signals.isArray()).isTrue();

        boolean hasTraces = false;
        for (JsonNode signal : signals) {
            if ("traces".equals(signal.get("key").asText())) {
                hasTraces = true;
                assertThat(signal.get("title").asText()).isEqualTo("OpenTelemetry Traces");
                assertThat(signal.get("pageId").asText()).isEqualTo("quarkus-opentelemetry/traces");
            }
        }
        assertThat(hasTraces)
                .as("OpenTelemetry should contribute a 'traces' signal to the Observability section")
                .isTrue();
    }
}
