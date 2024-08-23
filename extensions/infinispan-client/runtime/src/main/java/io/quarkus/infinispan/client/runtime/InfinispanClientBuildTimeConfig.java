package io.quarkus.infinispan.client.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * @author William Burns
 */
@ConfigGroup
public class InfinispanClientBuildTimeConfig {

    /**
     * Sets the bounded entry count for near cache. If this value is 0 or less near cache is disabled.
     *
     * @deprecated use per cache configuration for near cache max entries
     */
    @ConfigItem
    @Deprecated
    public int nearCacheMaxEntries;

    /**
     * Sets the marshallerClass. Default is ProtoStreamMarshaller
     */
    @ConfigItem
    public Optional<String> marshallerClass;

    /**
     * Configures caches build time config from the client with the provided configuration.
     */
    @ConfigItem
    public Map<String, InfinispanClientBuildTimeConfig.RemoteCacheConfig> cache = new HashMap<>();

    @ConfigGroup
    public static class RemoteCacheConfig {

        // @formatter:off
        /**
         * Cache configuration file in XML, JSON or YAML is defined in build time to create the cache on first access.
         * An example of the user defined property. cacheConfig.xml file is located in the 'resources' folder:
         * quarkus.infinispan-client.cache.bookscache.configuration-resource=cacheConfig.xml
         */
        // @formatter:on
        @ConfigItem
        public Optional<String> configurationResource;
    }

    /**
     * Dev Services.
     * <p>
     * Dev Services allows Quarkus to automatically start an Infinispan Server in dev and test
     * mode.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public DevServiceConfiguration devService;

    @ConfigGroup
    public static class DevServiceConfiguration {
        /**
         * Dev Services
         * <p>
         * Dev Services allows Quarkus to automatically start Infinispan in dev and test mode.
         */
        @ConfigItem
        @ConfigDocSection(generated = true)
        public InfinispanDevServicesConfig devservices;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            DevServiceConfiguration that = (DevServiceConfiguration) o;
            return Objects.equals(devservices, that.devservices);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devservices);
        }
    }

    @Override
    public String toString() {
        return "InfinispanClientBuildTimeConfig{" + "nearCacheMaxEntries=" + nearCacheMaxEntries + ", marshallerClass="
                + marshallerClass + ", cache=" + cache + ", devService=" + devService + '}';
    }
}
