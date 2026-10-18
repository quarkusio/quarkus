package io.quarkus.oidc.token.propagation.reactive.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.rest-client-oidc-token-propagation")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OidcTokenPropagationReactiveConfig {
    /**
     * Exchange the current token with OpenId Connect Provider for a new token using either
     * "urn:ietf:params:oauth:grant-type:token-exchange" or "urn:ietf:params:oauth:grant-type:jwt-bearer" token grant
     * before propagating it.
     */
    @WithDefault("false")
    boolean exchangeToken();

    /**
     * Name of the configured OidcClient.
     *
     * Note this property is only used if the `exchangeToken` property is enabled.
     */
    Optional<String> clientName();
}
