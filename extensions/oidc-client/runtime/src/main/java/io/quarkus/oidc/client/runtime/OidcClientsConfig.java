package io.quarkus.oidc.client.runtime;

import java.util.Map;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc-client", phase = ConfigPhase.RUN_TIME)
public class OidcClientsConfig {

    /**
     * The default client.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public OidcClientConfig defaultClient;

    /**
     * Additional named clients.
     */
    @ConfigDocSection
    @ConfigDocMapKey("id")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, OidcClientConfig> namedClients;
}
