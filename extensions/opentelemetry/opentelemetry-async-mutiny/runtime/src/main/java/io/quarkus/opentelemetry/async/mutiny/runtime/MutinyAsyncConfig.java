package io.quarkus.opentelemetry.async.mutiny.runtime;

import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.EventConfig;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.SpanAttributeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class MutinyAsyncConfig {

    @ConfigRoot(name = "opentelemetry.tracer.async.mutiny", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public static class MutinyAsyncBuildConfig {
        /**
         * Mutiny async support.
         * <p>
         * Mutiny async support is enabled by default.
         */
        @ConfigItem(defaultValue = "true")
        public Boolean enabled;
    }

    @ConfigRoot(name = "opentelemetry.tracer.async.mutiny", phase = ConfigPhase.RUN_TIME)
    public static class MutinyAsyncRuntimeConfig {

        /** Build / static runtime config for spanAttribute */
        public SpanAttributeConfig spanAttribute;

        /** Build / static runtime config for event */
        public EventConfig event;
    }
}
