package io.quarkus.opentelemetry.deployment.logs;

import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.opentelemetry.deployment.common.InMemoryLogRecordExporter;
import io.quarkus.opentelemetry.deployment.common.InMemoryLogRecordExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class OtelLogsDisabledTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .add(new StringAsset(
                                            "quarkus.otel.logs.enabled=false\n" +
                                                    "quarkus.otel.traces.enabled=false\n"),
                                    "application.properties"));


    @Inject
    Instance<LogRecordExporter> logRecordExporter;

    @Test
    public void testLogRecordExporter() {
        assertThat(logRecordExporter.isUnsatisfied()).isTrue();
    }
}
