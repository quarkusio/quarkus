package io.quarkus.resteasy.common.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.resteasy")
public interface ResteasyCommonConfig {

    /**
     * Enable gzip support for REST
     */
    ResteasyCommonConfigGzip gzip();

    interface ResteasyCommonConfigGzip {
        /**
         * If gzip is enabled
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Maximum deflated file bytes size, up to {@code Long.MAX_VALUE} bytes.
         * <p>
         * If the limit is exceeded, Resteasy will return Response
         * with status 413("Request Entity Too Large")
         */
        @WithDefault("10M")
        MemorySize maxInput();
    }

}
