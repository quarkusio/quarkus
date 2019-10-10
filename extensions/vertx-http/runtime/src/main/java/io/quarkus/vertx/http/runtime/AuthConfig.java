package io.quarkus.vertx.http.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Authentication mechanism and SecurityRealm name information used for configuring HTTP auth
 * instance for the deployment.
 */
@ConfigGroup
public class AuthConfig {
    /**
     * If basic auth should be enabled. If both basic and form auth is enabled then basic auth will be enabled in silent mode.
     *
     * If no authentication mechanisms are configured basic auth is the default, unless an
     * {@link io.quarkus.security.identity.IdentityProvider}
     * is present that supports {@link io.quarkus.security.identity.request.TokenAuthenticationRequest} in which case
     * form auth will be the default.
     */
    @ConfigItem
    public boolean basic;

    /**
     * If form auth should be enabled.
     */
    @ConfigItem
    public boolean form;

    /**
     * The authentication realm
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realm;

    /**
     * The HTTP permissions
     */
    @ConfigItem(name = "permission")
    public Map<String, PermissionSetConfig> permissions;

    /**
     * If this is true then any HTTP request that has not been explicitly permitted by a permission checker will be denied
     */
    @ConfigItem
    public boolean defaultDeny = false;
}
