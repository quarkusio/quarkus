package io.quarkus.infinispan.client.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
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
     * Named clients.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("client-name")
    @ConfigDocSection
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

    /**
     * Starts the client and connects to the server. If set to false, you'll need to start it yourself.
     */
    @ConfigItem(defaultValue = "true")
    public Optional<Boolean> startClient;

    public InfinispanClientRuntimeConfig getInfinispanClientRuntimeConfig(String infinispanClientName) {
        if (InfinispanClientUtil.isDefault(infinispanClientName)) {
            return defaultInfinispanClient;
        }
        return namedInfinispanClients.get(infinispanClientName);
    }
}
