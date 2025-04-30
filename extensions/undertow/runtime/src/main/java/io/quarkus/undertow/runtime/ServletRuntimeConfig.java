package io.quarkus.undertow.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.servlet")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ServletRuntimeConfig {

    /**
     * The buffer size to use for Servlet, up to {@code Integer.MAX_VALUE} bytes. If this is not specified the default will
     * depend on the amount
     * of available memory. If there is less than 64mb it will default to 512b heap buffer, less that 128mb
     * 1k direct buffer and otherwise 16k direct buffers.
     *
     */
    Optional<MemorySize> bufferSize();

    /**
     * If Servlet should use direct buffers, this gives maximum performance but can be problematic
     * in memory constrained environments
     */
    Optional<Boolean> directBuffers();

    /**
     * The maximum number of HTTP request parameters permitted for Servlet requests.
     *
     * If a client sends more than this number of parameters in a request, the connection is closed.
     */
    @WithDefault("1000")
    int maxParameters();
}
