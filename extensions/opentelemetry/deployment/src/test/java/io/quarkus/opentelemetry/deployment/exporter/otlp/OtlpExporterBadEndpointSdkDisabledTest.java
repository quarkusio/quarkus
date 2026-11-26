package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An invalid endpoint fails startup also when the SDK is disabled at run time.
 */
public class OtlpExporterBadEndpointSdkDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.traces.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.sdk.disabled", "true")
            .overrideConfigKey("quarkus.otel.exporter.otlp.endpoint", "httz://nada:zero")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageStartingWith("Invalid endpoint, must start with http:// or https://"));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void failStart() {
        Assertions.fail("Test should not be run as deployment should fail");
    }
}
