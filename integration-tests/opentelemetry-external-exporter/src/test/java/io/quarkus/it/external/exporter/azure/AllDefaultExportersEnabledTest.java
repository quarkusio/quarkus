package io.quarkus.it.external.exporter.azure;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.it.external.exporter.ExporterTestBase;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Verify that OpenTelemetry logs, traces and metrics are sent using CDI exporters when the default exporter
 * is enabled, and that the Azure exporter continues to export telemetry as well.
 */
@QuarkusTest
@TestProfile(AllDefaultExportersEnabledTest.DefaultExportersEnabledProfile.class)
public class AllDefaultExportersEnabledTest extends ExporterTestBase {

    @Test
    void telemetrySentUsingDefaultExporters() throws InterruptedException {
        when()
                .get("/hello")
                .then()
                .statusCode(200);

        // wait for telemetry to be sent to Application Insights via the /v2.1/track API endpoint
        await()
                .atMost(Duration.ofSeconds(10))
                .until(azureExporterTelemetry.telemetryDataContainExport());

        // wait a bit more to ensure all telemetry data has arrived
        Thread.sleep(10_000);

        // verify that telemetry was sent via the Azure exporter
        azureExporterTelemetry.verifyExportedTraces();
        azureExporterTelemetry.verifyExportedMetrics();
        azureExporterTelemetry.verifyExportedLogs();

        // verify that telemetry was sent via the default exporter
        defaultExporterTelemetry.verifyExportedTraces();
        defaultExporterTelemetry.verifyExportedMetrics();
        defaultExporterTelemetry.verifyExportedLogs();
    }

    public static class DefaultExportersEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    // azure exporter has runtime config in build steps
                    // https://github.com/quarkiverse/quarkus-opentelemetry-exporter/pull/383
                    "quarkus.extension-loader.report-runtime-config-at-deployment", "warn",
                    "quarkus.otel.exporter.otlp.default-exporter-enabled", "true");
        }
    }
}
