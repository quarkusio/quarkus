package io.quarkus.infinispan.client.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = InfinispanClientUtil.INFINISPAN_CLIENT_CONFIG_MAPPING_PREFIX)
public interface InfinispanClientsRuntimeConfig {

    /**
     * The default Infinispan Client.
     */
    @WithParentName
    InfinispanClientRuntimeConfig defaultInfinispanClient();

    /**
     * Named clients.
     */
    @WithParentName
    @ConfigDocMapKey("client-name")
    @ConfigDocSection
    Map<String, InfinispanClientRuntimeConfig> namedInfinispanClients();

    // @formatter:off
    /**
     * Enables or disables Protobuf generated schemas upload to the server.
     * Set it to 'false' when you need to handle the lifecycle of the Protobuf Schemas on Server side yourself.
     * Default is 'true'.
     * This is a global setting and is not specific to a Infinispan Client.
     */
    // @formatter:on
    @WithDefault("true")
    Optional<Boolean> useSchemaRegistration();

    /**
     * Starts the client and connects to the server. If set to false, you'll need to start it yourself.
     */
    @WithDefault("true")
    Optional<Boolean> startClient();

    default InfinispanClientRuntimeConfig getInfinispanClientRuntimeConfig(String infinispanClientName) {
        if (InfinispanClientUtil.isDefault(infinispanClientName)) {
            return defaultInfinispanClient();
        }
        return namedInfinispanClients().get(infinispanClientName);
    }
}
