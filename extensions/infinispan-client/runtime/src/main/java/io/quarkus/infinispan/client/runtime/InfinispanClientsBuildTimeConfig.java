package io.quarkus.infinispan.client.runtime;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_MAPPING_PREFIX)
public interface InfinispanClientsBuildTimeConfig {
    /**
     * The default Infinispan Client.
     */
    @WithParentName
    InfinispanClientBuildTimeConfig defaultInfinispanClient();

    /**
     * Named clients.
     */
    @WithParentName
    @ConfigDocMapKey("client-name")
    @ConfigDocSection
    Map<String, InfinispanClientBuildTimeConfig> namedInfinispanClients();

    /**
     * Whether or not a health check is published in case the smallrye-health extension is present.
     * <p>
     * This is a global setting and is not specific to an Infinispan Client.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();

    default Set<String> getInfinispanNamedClientConfigNames() {
        return Collections.unmodifiableSet(new HashSet<>(namedInfinispanClients().keySet()));
    }

    default InfinispanClientBuildTimeConfig getInfinispanClientBuildTimeConfig(String infinispanClientName) {
        if (InfinispanClientUtil.isDefault(infinispanClientName)) {
            return defaultInfinispanClient();
        }
        return namedInfinispanClients().get(infinispanClientName);
    }
}
