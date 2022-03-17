package io.quarkus.stork;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Stork configuration root.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class StorkConfiguration {

    /**
     * ServiceDiscovery configuration for the service
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocSection
    @ConfigDocMapKey("service-name")
    public Map<String, ServiceConfiguration> serviceConfiguration;

}
