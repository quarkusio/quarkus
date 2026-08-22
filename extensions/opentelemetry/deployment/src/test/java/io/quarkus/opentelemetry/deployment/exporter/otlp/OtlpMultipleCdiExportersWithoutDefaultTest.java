package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that two independent, purely user-provided CDI LogRecordExporter beans
 * (neither of which is the Quarkus default) can coexist via CompositeLogRecordExporter,
 * without quarkus.otel.experimental.otlp.default.enable being involved at all. This
 * confirms the AmbiguousResolutionException fix in LogsExporterCDIProvider handles the
 * general multi-Quarkiverse-exporter case, not just default+custom coexistence.
 */
public class OtlpMultipleCdiExportersWithoutDefaultTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .withApplicationRoot(root -> root.addClasses(TwoLogExportersProducer.class))
            .overrideConfigKey("quarkus.otel.logs.enabled", "true")
            .overrideConfigKey("quarkus.otel.logs.exporter", "cdi");

    @Inject
    BeanManager beanManager;

    @Test
    void twoUserProvidedExportersCoexistWithoutDefaultFlag() {
        var beans = beanManager.getBeans(LogRecordExporter.class);
        assertEquals(2, beans.size(),
                "Both user-provided LogRecordExporter beans should be resolvable as beans; "
                        + "found: " + beans);
    }

    public static class TwoLogExportersProducer {
        @Produces
        @Singleton
        public LogRecordExporter firstLogExporter() {
            return InMemoryLogRecordExporter.create();
        }

        @Produces
        @Singleton
        public LogRecordExporter secondLogExporter() {
            return new LogRecordExporter() {
                @Override
                public CompletableResultCode export(Collection<LogRecordData> logs) {
                    return CompletableResultCode.ofSuccess();
                }

                @Override
                public CompletableResultCode flush() {
                    return CompletableResultCode.ofSuccess();
                }

                @Override
                public CompletableResultCode shutdown() {
                    return CompletableResultCode.ofSuccess();
                }
            };
        }
    }
}
