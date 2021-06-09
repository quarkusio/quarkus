package io.quarkus.oidc.token.propagation.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc-token-propagation", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OidcTokenPropagationConfig {
    /**
     * Enable either AccessTokenRequestFilter or JsonWebTokenRequestFilter for all the injected MP RestClient implementations.
     *
     * AccessTokenRequestFilter can propagate both opaque (binary) and JsonWebToken tokens but it can not modify
     * and secure the updated JsonWebToken tokens.
     * JsonWebTokenRequestFilter can only propagate JsonWebToken tokens but it can also modify and secure them again.
     * Enable the 'jsonWebToken' property to have JsonWebTokenRequestFilter registered.
     *
     * Alternatively, instead of using this property for registering these filters with all the injected MP RestClient
     * implementations, both filters can be registered as MP RestClient providers with the specific MP RestClient
     * implementations.
     */
    @ConfigItem(defaultValue = "false")
    public boolean registerFilter;

    /**
     * Enable JsonWebTokenRequestFilter instead of AccessTokenRequestFilter for all the injected MP RestClient implementations.
     * This filter can propagate as well as modify and secure the updated JsonWebToken tokens.
     *
     * Note this property is ignored unless the 'registerFilter' property is enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean jsonWebToken;

    /**
     * Secure the injected and possibly modified JsonWebToken.
     * For example, a JsonWebToken produced and signed by OpenId Connect provider can be re-signed using a new private key.
     *
     * Note this property is injected into JsonWebTokenRequestFilter.
     */
    @ConfigItem(defaultValue = "false")
    public boolean secureJsonWebToken;

    /**
     * Exchange the current token with OpenId Connect Provider for a new token before propagating it.
     *
     * Note this property is injected into AccessTokenRequestFilter.
     */
    @ConfigItem(defaultValue = "false")
    public boolean exchangeToken;

    /**
     * Name of the configured OidcClient.
     * 
     * Note this property is injected into AccessTokenRequestFilter and is only used if the `exchangeToken` property is enabled.
     */
    @ConfigItem
    public Optional<String> clientName;
}
