package io.quarkus.infinispan.client.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * @author William Burns
 */
@ConfigGroup
public interface InfinispanClientBuildTimeConfig {

    /**
     * Sets the bounded entry count for near cache. If this value is 0 or less near cache is disabled.
     *
     * @deprecated use per cache configuration for near cache max entries
     */
    @Deprecated
    @WithDefault("0")
    int nearCacheMaxEntries();

    /**
     * Sets the marshallerClass. Default is ProtoStreamMarshaller
     */
    Optional<String> marshallerClass();

    /**
     * Configures caches build time config from the client with the provided configuration.
     */
    Map<String, InfinispanClientBuildTimeConfig.RemoteCacheConfig> cache();

    @ConfigGroup
    public interface RemoteCacheConfig {

        // @formatter:off
        /**
         * Cache configuration file in XML, JSON or YAML is defined in build time to create the cache on first access.
         * An example of the user defined property. cacheConfig.xml file is located in the 'resources' folder:
         * quarkus.infinispan-client.cache.bookscache.configuration-resource=cacheConfig.xml
         */
        // @formatter:on
        Optional<String> configurationResource();
    }

    /**
     * Dev Services.
     * <p>
     * Dev Services allows Quarkus to automatically start an Infinispan Server in dev and test
     * mode.
     */
    @WithParentName
    DevServiceConfiguration devservices();

    @ConfigGroup
    public interface DevServiceConfiguration {
        /**
         * Dev Services
         * <p>
         * Dev Services allows Quarkus to automatically start Infinispan in dev and test mode.
         */
        @ConfigDocSection(generated = true)
        InfinispanDevServicesConfig devservices();
    }
}
