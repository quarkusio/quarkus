package io.quarkus.oidc.client.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.oidc-client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OidcClientsConfig {

    /**
     * The default client.
     */
    @WithParentName
    OidcClientConfig defaultClient();

    /**
     * Additional named clients.
     */
    @ConfigDocSection
    @ConfigDocMapKey("id")
    @WithParentName
    Map<String, OidcClientConfig> namedClients();
}
