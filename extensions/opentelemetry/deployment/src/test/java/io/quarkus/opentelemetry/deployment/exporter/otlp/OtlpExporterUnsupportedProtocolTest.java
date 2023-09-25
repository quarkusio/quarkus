package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class OtlpExporterUnsupportedProtocolTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("otel.traces.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.exporter.otlp.traces.protocol", "http/protobuf")
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.otlp.endpoint", "http://localhost ")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "50")
            .overrideConfigKey("quarkus.otel.bsp.export.timeout", "1s")
            .assertException(t -> {
                Throwable e = t;
                IllegalStateException ie = null;
                while (e != null) {
                    if (t instanceof IllegalStateException) {
                        ie = (IllegalStateException) t;
                        break;
                    }
                    e = e.getCause();
                }

                if (ie == null) {
                    fail("No IllegalStateException thrown: " + t);
                }
                assertTrue(ie.getMessage().contains("Only the `grpc` protocol is currently supported."),
                        ie.getMessage());
                assertTrue(ie.getMessage().contains("`http/protobuf` is available after Quarkus 3.3."),
                        ie.getMessage());
                assertTrue(ie.getMessage().contains("Please check `otel.exporter.otlp.traces.protocol` property"),
                        ie.getMessage());
            });

    @Test
    void testNotSupportedProtocolConfig() {
        fail();
    }
}
