package io.quarkus.oidc.client.reactive.filter.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc-client-reactive-filter", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OidcClientReactiveFilterConfig {

    /**
     * Name of the configured OidcClient used by the OidcClientRequestReactiveFilter. You can override this configuration
     * for individual MP RestClients with the `io.quarkus.oidc.client.filter.OidcClientFilter` annotation.
     */
    @ConfigItem
    public Optional<String> clientName;
}
