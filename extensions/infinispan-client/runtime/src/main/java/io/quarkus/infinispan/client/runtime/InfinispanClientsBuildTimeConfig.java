package io.quarkus.infinispan.client.runtime;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_ROOT_NAME, phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class InfinispanClientsBuildTimeConfig {
    /**
     * The default Infinispan Client.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public InfinispanClientBuildTimeConfig defaultInfinispanClient;

    /**
     * Named clients.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("client-name")
    @ConfigDocSection
    public Map<String, InfinispanClientBuildTimeConfig> namedInfinispanClients;

    /**
     * Whether or not a health check is published in case the smallrye-health extension is present.
     * <p>
     * This is a global setting and is not specific to an Infinispan Client.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    public Set<String> getInfinispanNamedClientConfigNames() {
        return Collections.unmodifiableSet(new HashSet<>(namedInfinispanClients.keySet()));
    }

    public InfinispanClientBuildTimeConfig getInfinispanClientBuildTimeConfig(String infinispanClientName) {
        if (InfinispanClientUtil.isDefault(infinispanClientName)) {
            return defaultInfinispanClient;
        }
        return namedInfinispanClients.get(infinispanClientName);
    }
}
