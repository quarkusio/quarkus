package io.quarkus.opentelemetry.runtime.config.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface BatchLogRecordProcessorConfig {

    /**
     * The Batch Log Record Processor interval {@link Duration} between two consecutive exports.
     * <p>
     * Default is `1S`.
     */
    @WithName("schedule.delay")
    @WithDefault("1s")
    Duration scheduleDelay();

    /**
     * The Batch Log Record Processor maximum queue size.
     * <p>
     * Default is `2048`.
     */
    @WithName("max.queue.size")
    @WithDefault("2048")
    Integer maxQueueSize();

    /**
     * The Batch Log Record Processor maximum batch size.
     * <p>
     * Default is `512`.
     */
    @WithName("max.export.batch.size")
    @WithDefault("512")
    Integer maxExportBatchSize();

    /**
     * The Batch Log Record Processor maximum allowed time {@link Duration} to export data.
     * <p>
     * Default is `30s`.
     */
    @WithName("export.timeout")
    @WithDefault("30s")
    Duration exportTimeout();
}
