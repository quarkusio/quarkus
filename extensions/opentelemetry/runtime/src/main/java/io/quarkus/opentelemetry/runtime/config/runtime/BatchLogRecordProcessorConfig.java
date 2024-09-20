package io.quarkus.opentelemetry.runtime.config.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface BatchLogRecordProcessorConfig {

    /**
     * The interval, in milliseconds, between two consecutive exports.
     * <p>
     * Default is `1000`.
     */
    @WithName("schedule.delay")
    @WithDefault("1s")
    Duration scheduleDelay();

    /**
     * The maximum queue size.
     * <p>
     * Default is `2048`.
     */
    @WithName("max.queue.size")
    @WithDefault("2048")
    Integer maxQueueSize();

    /**
     * The maximum batch size.
     * <p>
     * Default is `512`.
     */
    @WithName("max.export.batch.size")
    @WithDefault("512")
    Integer maxExportBatchSize();

    /**
     * The maximum allowed time, in milliseconds, to export data.
     * <p>
     * Default is `30s`.
     */
    @WithName("export.timeout")
    @WithDefault("30s")
    Duration exportTimeout();
}
