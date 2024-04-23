package io.quarkus.cache.infinispan.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = BUILD_AND_RUN_TIME_FIXED, name = "cache.infinispan")
public class InfinispanCachesBuildTimeConfig {

    /**
     * The name of the named Infinispan client to be used for communicating with Infinispan.
     * If not set, use the default Infinispan client.
     */
    @ConfigItem
    public Optional<String> clientName;
}
