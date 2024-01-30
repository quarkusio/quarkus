package io.quarkus.oidc.proxy.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.oidc-proxy")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OidcProxyConfig {

    /**
     * OIDC proxy authorization endpoint path
     */
    @WithDefault("/q/oidc")
    String rootPath();

    /**
     * OIDC proxy authorization endpoint path relative to the {@link #rootPath()}.
     */
    @WithDefault("/authorize")
    String authorizationPath();

    /**
     * OIDC proxy token endpoint path relative to the {@link #rootPath()}
     */
    @WithDefault("/token")
    String tokenPath();

    /**
     * OIDC proxy JSON Web Key Set endpoint path relative to the {@link #rootPath()}
     */
    @WithDefault("/jwks")
    String jwksPath();

    /**
     * OIDC proxy UserInfo endpoint path relative to the {@link #rootPath()}.
     * This path will not be supported if {@link #allowIdToken()} is set to `false`.
     */
    @WithDefault("/userinfo")
    String userInfoPath();

    /**
     * OIDC service tenant identifier
     */
    Optional<String> tenantId();

    /**
     * Absolute external redirect URI.
     * <p/>
     * If 'quarkus.oidc.authentication.redirect-path' is configured then configuring this proxy is required.
     * In this case, the proxy will request a redirect to 'quarkus.oidc.authentication.redirect-path' and
     * will redirect further to the external config path.
     */
    Optional<String> externalRedirectUri();

    /**
     * Allow to return a refresh token from the authorization code grant response
     */
    @WithDefault("true")
    boolean allowRefreshToken();

    /**
     * Allow to return an ID token from the authorization code grant response
     */
    @WithDefault("true")
    boolean allowIdToken();

    /**
     * Require that if the OIDC `service` application configures the client id
     * then the provided client id must match it
     */
    @WithDefault("true")
    boolean clientIdMatchRequired();

    /**
     * Require that if the OIDC `service` application configures the client secret
     * then the provided client secret must match it
     */
    @WithDefault("true")
    boolean clientSecretMatchRequired();
}
