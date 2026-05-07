package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class BatchSpanProcessorConfigTest {

    private static final Duration SCHEDULE_DELAY = Duration.ofMillis(100);
    private static final int MAX_QUEUE_SIZE = 1024;
    private static final int MAX_EXPORT_BATCH_SIZE = 256;
    private static final Duration EXPORT_TIMEOUT = Duration.ofSeconds(10);

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class))
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", SCHEDULE_DELAY.toMillis() + "ms")
            .overrideConfigKey("quarkus.otel.bsp.max.queue.size", String.valueOf(MAX_QUEUE_SIZE))
            .overrideConfigKey("quarkus.otel.bsp.max.export.batch.size", String.valueOf(MAX_EXPORT_BATCH_SIZE))
            .overrideConfigKey("quarkus.otel.bsp.export.timeout", EXPORT_TIMEOUT.toMillis() + "ms");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void batchSpanProcessorHasCorrectScheduleDelay() throws Exception {
        BatchSpanProcessor bsp = TestUtil.getBatchSpanProcessor(openTelemetry);
        assertNotNull(bsp, "BatchSpanProcessor should be present");

        long actualNanos = TestUtil.getBspScheduleDelayNanos(bsp);
        assertEquals(SCHEDULE_DELAY.toNanos(), actualNanos,
                "scheduleDelay should match the configured value");
    }

    @Test
    void batchSpanProcessorHasCorrectMaxQueueSize() throws Exception {
        BatchSpanProcessor bsp = TestUtil.getBatchSpanProcessor(openTelemetry);
        assertNotNull(bsp, "BatchSpanProcessor should be present");

        int actualMaxQueueSize = TestUtil.getBspMaxQueueSize(bsp);
        assertEquals(MAX_QUEUE_SIZE, actualMaxQueueSize,
                "maxQueueSize should match the configured value");
    }

    @Test
    void batchSpanProcessorHasCorrectMaxExportBatchSize() throws Exception {
        BatchSpanProcessor bsp = TestUtil.getBatchSpanProcessor(openTelemetry);
        assertNotNull(bsp, "BatchSpanProcessor should be present");

        int actualMaxExportBatchSize = TestUtil.getBspMaxExportBatchSize(bsp);
        assertEquals(MAX_EXPORT_BATCH_SIZE, actualMaxExportBatchSize,
                "maxExportBatchSize should match the configured value");
    }

    @Test
    void batchSpanProcessorHasCorrectExportTimeout() throws Exception {
        BatchSpanProcessor bsp = TestUtil.getBatchSpanProcessor(openTelemetry);
        assertNotNull(bsp, "BatchSpanProcessor should be present");

        long actualTimeoutNanos = TestUtil.getBspExporterTimeoutNanos(bsp);
        assertEquals(EXPORT_TIMEOUT.toNanos(), actualTimeoutNanos,
                "exportTimeout should match the configured value");
    }
}
