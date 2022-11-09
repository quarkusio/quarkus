package io.quarkus.oidc.token.propagation.reactive;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc-token-propagation-reactive", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OidcTokenPropagationReactiveConfig {
    /**
     * Exchange the current token with OpenId Connect Provider for a new token using either
     * "urn:ietf:params:oauth:grant-type:token-exchange" or "urn:ietf:params:oauth:grant-type:jwt-bearer" token grant
     * before propagating it.
     */
    @ConfigItem(defaultValue = "false")
    public boolean exchangeToken;

    /**
     * Name of the configured OidcClient.
     *
     * Note this property is only used if the `exchangeToken` property is enabled.
     */
    @ConfigItem
    public Optional<String> clientName;
}
