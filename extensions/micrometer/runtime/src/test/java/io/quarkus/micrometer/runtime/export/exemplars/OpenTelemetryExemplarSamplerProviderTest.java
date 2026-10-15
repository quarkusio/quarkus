package io.quarkus.micrometer.runtime.export.exemplars;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OpenTelemetryExemplarSamplerProviderTest {

    private static final Logger USAGE_LOGGER = Logger.getLogger("io.opentelemetry.usage");

    private final List<LogRecord> usageRecords = new CopyOnWriteArrayList<>();
    private final Handler recordingHandler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            usageRecords.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };
    private Level originalLevel;

    @BeforeEach
    void installRecordingHandler() {
        originalLevel = USAGE_LOGGER.getLevel();
        USAGE_LOGGER.setLevel(Level.ALL);
        USAGE_LOGGER.addHandler(recordingHandler);
    }

    @AfterEach
    void removeRecordingHandler() {
        USAGE_LOGGER.removeHandler(recordingHandler);
        USAGE_LOGGER.setLevel(originalLevel);
    }

    @Test
    void noOpenTelemetryApiUsageComplaintWhenNoContextIsActive() {
        // outside any Vert.x/OpenTelemetry context, e.g. registry setup on the main thread
        var spanContext = new OpenTelemetryExemplarSamplerProvider().exemplarSampler().orElseThrow();

        assertNull(spanContext.getCurrentTraceId());
        assertNull(spanContext.getCurrentSpanId());
        assertFalse(spanContext.isCurrentSpanSampled());

        assertTrue(usageRecords.isEmpty(),
                "querying the exemplar sampler without an active context should not trip the OpenTelemetry API usage logger, but got: "
                        + usageRecords.stream().map(LogRecord::getMessage).toList());
    }
}
