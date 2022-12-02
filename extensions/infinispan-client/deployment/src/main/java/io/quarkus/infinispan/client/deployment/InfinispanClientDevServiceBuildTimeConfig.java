package io.quarkus.infinispan.client.deployment;

import java.util.Objects;

import io.quarkus.infinispan.client.deployment.devservices.InfinispanDevServicesConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "infinispan-client", phase = ConfigPhase.BUILD_TIME)
public class InfinispanClientDevServiceBuildTimeConfig {

    /**
     * Default Dev services configuration.
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
}
