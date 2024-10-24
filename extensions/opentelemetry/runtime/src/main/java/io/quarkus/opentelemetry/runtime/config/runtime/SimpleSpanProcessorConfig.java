package io.quarkus.opentelemetry.runtime.config.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface SimpleSpanProcessorConfig {

    /**
     * Do we use simple or batch, default is batch,
     * since this value is false by default.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Whether unsampled spans should be exported.
     * <p>
     * Default is `false`.
     */
    @WithName("export.unsampled.spans")
    @WithDefault("false")
    boolean exportUnsampledSpans();
}
