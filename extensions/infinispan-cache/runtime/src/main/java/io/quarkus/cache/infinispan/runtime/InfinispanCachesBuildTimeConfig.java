package io.quarkus.cache.infinispan.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.cache.infinispan")
public interface InfinispanCachesBuildTimeConfig {

    /**
     * The name of the named Infinispan client to be used for communicating with Infinispan.
     * If not set, use the default Infinispan client.
     */
    Optional<String> clientName();
}
