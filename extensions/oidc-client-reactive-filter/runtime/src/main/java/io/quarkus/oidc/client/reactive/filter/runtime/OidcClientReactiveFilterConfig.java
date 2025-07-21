package io.quarkus.oidc.client.reactive.filter.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.rest-client-oidc-filter")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OidcClientReactiveFilterConfig {

    /**
     * Name of the configured OidcClient used by the OidcClientRequestReactiveFilter. You can override this configuration
     * for individual MP RestClients with the `io.quarkus.oidc.client.filter.OidcClientFilter` annotation.
     */
    Optional<String> clientName();

    /**
     * If Quarkus should refresh the access token after the MP REST client request results in 401 Unauthorized error.
     * The refresh can be useful when the access token can be revoked by other services while the access token
     * still appears valid locally.
     */
    @WithDefault("false")
    boolean refreshOnUnauthorized();
}
