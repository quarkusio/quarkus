package io.quarkus.opentelemetry.exporter.otlp.deployment;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusUnitTest;

public class OtlpExporterDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.otlp.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        Assertions.assertNotNull(openTelemetry);
    }
}
