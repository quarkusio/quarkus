package io.quarkus.resteasy.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.resteasy.vertx")
public interface ResteasyVertxConfig {

    /**
     * The size of the output stream response buffer. If a response is larger than this and no content-length
     * is provided then the request will be chunked.
     *
     * Larger values may give slight performance increases for large responses, at the expense of more memory usage.
     */
    @WithDefault("8191")
    int responseBufferSize();
}
