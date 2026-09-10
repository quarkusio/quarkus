package io.quarkus.opentelemetry.deployment.devui;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * Dev mode with the feature explicitly turned off: the dev-only build step returns
 * early, so the JSON-RPC service is never registered and its methods are unreachable.
 * Proves the build-time enabled=false gate actually removes the capture path (unlike a
 * TEST-mode check, which skips the build step regardless of enabled).
 */
public class OpenTelemetryDevUIDisabledTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addAsResource(new StringAsset(
                            "quarkus.dev-ui.observability.traces.enabled=false\n"
                                    + "quarkus.otel.traces.exporter=none\n"
                                    + "quarkus.otel.metrics.exporter=none\n"
                                    + "quarkus.otel.logs.exporter=none\n"
                                    + "quarkus.devservices.enabled=false\n"),
                            "application.properties"));

    public OpenTelemetryDevUIDisabledTest() {
        super("quarkus-opentelemetry");
    }

    @Test
    public void snapshotMethodIsUnavailableWhenDisabled() {
        // The service is not registered -> invoking its JSON-RPC method must not succeed.
        assertThatThrownBy(() -> super.executeJsonRPCMethod("getSnapshot"));
    }
}
