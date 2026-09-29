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

        String pageId = null;
        for (JsonNode signal : signals) {
            if ("traces".equals(signal.get("key").asText())) {
                assertThat(signal.get("title").asText()).isEqualTo("OpenTelemetry Traces");
                pageId = signal.get("pageId").asText();
            }
        }
        assertThat(pageId)
                .as("OpenTelemetry should contribute a 'traces' signal, with a page, to the Observability section")
                .isNotBlank();

        // The core traces view is an unlisted page, which the dashboard embeds in the card and opens full page.
        JsonNode unlistedPages = super.getBuildTimeData("unlistedPages");
        JsonNode tracesPage = null;
        for (JsonNode page : unlistedPages) {
            if (pageId.equals(page.get("id").asText())) {
                tracesPage = page;
            }
        }
        assertThat(tracesPage).as("the traces signal's page %s should be an unlisted page", pageId).isNotNull();
        assertThat(tracesPage.get("componentName").asText()).isEqualTo("qwc-observability-traces");
        assertThat(tracesPage.get("componentRef").asText()).endsWith("/qwc/qwc-observability-traces.js");
    }
}
