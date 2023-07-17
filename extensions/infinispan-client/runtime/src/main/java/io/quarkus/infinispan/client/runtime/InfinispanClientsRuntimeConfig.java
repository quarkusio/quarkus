package io.quarkus.infinispan.client.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_ROOT_NAME, phase = ConfigPhase.RUN_TIME)
public class InfinispanClientsRuntimeConfig {

    /**
     * The default Infinispan Client.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public InfinispanClientRuntimeConfig defaultInfinispanClient;

    /**
     * Additional named Infinispan Client.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, InfinispanClientRuntimeConfig> namedInfinispanClients;

    // @formatter:off
    /**
     * Enables or disables Protobuf generated schemas upload to the server.
     * Set it to 'false' when you need to handle the lifecycle of the Protobuf Schemas on Server side yourself.
     * Default is 'true'.
     * This is a global setting and is not specific to a Infinispan Client.
     */
    // @formatter:on
    @ConfigItem(defaultValue = "true")
    Optional<Boolean> useSchemaRegistration;

    public InfinispanClientRuntimeConfig getInfinispanClientRuntimeConfig(String infinispanClientName) {
        if (InfinispanClientUtil.isDefault(infinispanClientName)) {
            return defaultInfinispanClient;
        }
        return namedInfinispanClients.get(infinispanClientName);
    }
}
