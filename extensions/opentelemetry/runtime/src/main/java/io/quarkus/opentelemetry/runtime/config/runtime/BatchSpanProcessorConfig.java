package io.quarkus.opentelemetry.runtime.config.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface BatchSpanProcessorConfig {

    /**
     * The Batch Span Processor interval {@link Duration} between two consecutive exports.
     * <p>
     * Default is `5s`.
     */
    @WithName("schedule.delay")
    @WithDefault("5s")
    Duration scheduleDelay();

    /**
     * The Batch Span Processor maximum queue size.
     * <p>
     * Default is `2048`.
     */
    @WithName("max.queue.size")
    @WithDefault("2048")
    Integer maxQueueSize();

    /**
     * The Batch Span Processor maximum batch size.
     * <p>
     * Default is `512`.
     */
    @WithName("max.export.batch.size")
    @WithDefault("512")
    Integer maxExportBatchSize();

    /**
     * The Batch Span Processor maximum allowed time {@link Duration} to export data.
     * <p>
     * Default is `30s`.
     */
    @WithName("export.timeout")
    @WithDefault("30s")
    Duration exportTimeout();
}
