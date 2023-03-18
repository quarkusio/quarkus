package io.quarkus.opentelemetry.runtime.config.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class BatchSpanProcessorConfig {

    /**
     * The interval, in milliseconds, between two consecutive exports.
     * <p>
     * Default is 5000.
     */
    @ConfigItem(name = "schedule.delay", defaultValue = "5s")
    public Duration scheduleDelay;

    /**
     * The maximum queue size.
     * <p>
     * Default is 2048.
     */
    @ConfigItem(name = "max.queue.size", defaultValue = "2048")
    public Integer maxQueueSize;

    /**
     * The maximum batch size.
     * <p>
     * Default is 512.
     */
    @ConfigItem(name = "max.export.batch.size", defaultValue = "512")
    public Integer maxExportBatchSize;

    /**
     * The maximum allowed time, in milliseconds, to export data.
     * <p>
     * Default is 30_000.
     */
    @ConfigItem(name = "export.timeout", defaultValue = "30s")
    public Duration exportTimeout;
}
