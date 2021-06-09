package io.quarkus.redis.client.deployment;

import java.util.Map;
import java.util.Objects;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class RedisBuildTimeConfig {
    /**
     * Whether or not an health check is published in case the smallrye-health extension is present.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    /**
     * Default Dev services configuration.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public DevServiceConfiguration defaultDevService;

    /**
     * Additional dev services configurations
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("additional-redis-clients")
    public Map<String, DevServiceConfiguration> additionalDevServices;

    @ConfigGroup
    public static class DevServiceConfiguration {
        /**
         * Configuration for DevServices
         * <p>
         * DevServices allows Quarkus to automatically start Redis in dev and test mode.
         */
        @ConfigItem
        public DevServicesConfig devservices;

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
}
