package io.quarkus.rest.data.panache.runtime;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.rest-data-panache")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface RestDataPanacheConfig {

    /**
     * The maximum page size that can be requested via the `size` query parameter.
     * If a client requests a larger page size, the request will be rejected with a 400 Bad Request.
     * Set to empty to disable the limit.
     */
    @WithDefault("100")
    OptionalInt maxPageSize();
}
