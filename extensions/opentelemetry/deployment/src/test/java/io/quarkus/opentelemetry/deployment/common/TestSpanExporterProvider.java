package io.quarkus.opentelemetry.deployment.common;

import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.test.QuarkusUnitTest;

public class TestSpanExporterProvider implements ConfigurableSpanExporterProvider {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.devservices.enabled", "false");

    @Override
    public SpanExporter createExporter(final ConfigProperties config) {
        return CDI.current().select(TestSpanExporter.class).get();
    }

    @Override
    public String getName() {
        return "test-span-exporter";
    }
}
