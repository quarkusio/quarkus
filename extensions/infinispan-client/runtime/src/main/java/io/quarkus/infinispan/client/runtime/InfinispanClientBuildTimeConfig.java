package io.quarkus.infinispan.client.runtime;

import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * @author William Burns
 */
@ConfigGroup
public class InfinispanClientBuildTimeConfig {

    /**
     * Sets the bounded entry count for near cache. If this value is 0 or less near cache is disabled.
     */
    @ConfigItem
    public int nearCacheMaxEntries;

    /**
     * Sets the marshallerClass. Default is ProtoStreamMarshaller
     */
    @ConfigItem
    public Optional<String> marshallerClass;

    /**
     * Configuration for DevServices. DevServices allows Quarkus to automatically start an Infinispan Server in dev and test
     * mode.
     */

    @ConfigItem(name = ConfigItem.PARENT)
    public DevServiceConfiguration devService;

    @ConfigGroup
    public static class DevServiceConfiguration {
        /**
         * Configuration for DevServices
         * <p>
         * DevServices allows Quarkus to automatically start Infinispan in dev and test mode.
         */
        @ConfigItem
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
        return "InfinispanClientBuildTimeConfig{" +
                "nearCacheMaxEntries=" + nearCacheMaxEntries + '}';
    }
}
