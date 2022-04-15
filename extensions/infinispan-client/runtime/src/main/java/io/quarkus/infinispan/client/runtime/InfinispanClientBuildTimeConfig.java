package io.quarkus.infinispan.client.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * @author William Burns
 */
@ConfigRoot(name = "infinispan-client", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class InfinispanClientBuildTimeConfig {

    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    /**
     * Sets the bounded entry count for near cache. If this value is 0 or less near cache is disabled.
     */
    @ConfigItem
    public int nearCacheMaxEntries;

    @Override
    public String toString() {
        return "InfinispanClientBuildTimeConfig{" +
                "healthEnabled=" + healthEnabled + ", nearCacheMaxEntries="
                + nearCacheMaxEntries + '}';
    }
}
