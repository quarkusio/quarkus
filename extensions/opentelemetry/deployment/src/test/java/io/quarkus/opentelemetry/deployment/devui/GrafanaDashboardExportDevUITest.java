package io.quarkus.opentelemetry.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import tools.jackson.databind.JsonNode;

/**
 * The Grafana export over JSON-RPC, for an application whose metrics leave over OTLP: the queries have to
 * use the names Prometheus gives them on receipt, not the meter names the dashboard shows.
 */
public class GrafanaDashboardExportDevUITest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(CustomMetricResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.application.name=orders\n"
                                    + "quarkus.otel.metrics.enabled=true\n"
                                    + "quarkus.otel.traces.enabled=false\n"),
                            "application.properties"));

    public GrafanaDashboardExportDevUITest() {
        super("devui-observability");
    }

    @Test
    public void exportsTheCardsAsAGrafanaDashboard() throws Exception {
        JsonNode dashboard = super.executeJsonRPCMethod("exportGrafanaDashboard", Map.of(
                "cards", List.of(
                        Map.of("kind", "metric", "name", "custom.otel.hits", "plot", "rate", "unit", "{hit}"),
                        Map.of("kind", "signal", "id", "traces", "title", "Traces")),
                "naming", "otlp",
                "title", "Orders"));

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.get("title").asText()).isEqualTo("Orders");

        JsonNode panels = dashboard.get("panels");
        assertThat(panels.size()).isEqualTo(2);
        assertThat(panels.get(0).get("targets").get(0).get("expr").asText())
                .isEqualTo("rate(custom_otel_hits_total[$__rate_interval])");
        assertThat(panels.get(1).get("datasource").get("type").asText()).isEqualTo("tempo");
        assertThat(panels.get(1).get("targets").get(0).get("datasource").get("type").asText()).isEqualTo("tempo");
        // The application name, not the artifact name, is what the traces carry as their service name.
        assertThat(panels.get(1).get("targets").get(0).get("query").asText())
                .isEqualTo("{ resource.service.name = \"orders\" }");
    }

    @Test
    public void exportsAnEmptyDashboardWhenNoCardsAreSent() throws Exception {
        JsonNode dashboard = super.executeJsonRPCMethod("exportGrafanaDashboard", Map.of(
                "cards", List.of(), "naming", "otlp", "title", ""));

        assertThat(dashboard.get("panels").isEmpty()).isTrue();
        assertThat(dashboard.get("title").asText()).isEqualTo("Quarkus Dev UI observability");
    }
}
