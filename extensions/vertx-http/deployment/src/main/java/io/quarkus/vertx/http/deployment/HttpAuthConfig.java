package io.quarkus.vertx.http.deployment;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Authentication mechanism and SecurityRealm name information used for configuring HTTP auth
 * instance for the deployment.
 */
@ConfigRoot
public class HttpAuthConfig {
    /**
     * If basic auth should be enabled. If both basic and form auth is enabled then basic auth will be enabled in silent mode.
     *
     * If no authentication mechanisms are configured basic auth is the default, unless an
     * {@link io.quarkus.security.identity.IdentityProvider}
     * is present that supports {@link io.quarkus.security.identity.request.TokenAuthenticationRequest} in which case
     * form auth will be the default.
     */
    @ConfigProperty(defaultValue = "false")
    public boolean basic;

    /**
     * If form auth should be enabled.
     */
    @ConfigProperty(defaultValue = "false")
    public boolean form;

    /**
     * The authentication realm
     */
    @ConfigProperty(name = "realm", defaultValue = "Quarkus")
    public String realm;
}
