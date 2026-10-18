package io.quarkus.oidc.client.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.oidc-client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OidcClientsConfig {

    String DEFAULT_CLIENT_KEY = "<default>";

    /**
     * Additional named clients.
     */
    @ConfigDocMapKey("id")
    @WithParentName
    @WithUnnamedKey(DEFAULT_CLIENT_KEY)
    @WithDefaults
    Map<String, OidcClientConfig> namedClients();

    static OidcClientConfig getDefaultClient(OidcClientsConfig config) {
        return config.namedClients().get(DEFAULT_CLIENT_KEY);
    }
}
