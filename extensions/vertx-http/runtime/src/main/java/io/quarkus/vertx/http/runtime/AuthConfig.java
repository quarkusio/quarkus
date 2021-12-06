package io.quarkus.vertx.http.runtime;

import java.util.Map;
import java.util.Optional;

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
     * If no authentication mechanisms are configured basic auth is the default.
     */
    @ConfigItem
    public Optional<Boolean> basic;

    /**
     * Form Auth config
     */
    @ConfigItem
    public FormAuthConfig form;

    /**
     * The authentication realm
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realm;

    /**
     * The HTTP permissions
     */
    @ConfigItem(name = "permission")
    public Map<String, PolicyMappingConfig> permissions;

    /**
     * The HTTP role based policies
     */
    @ConfigItem(name = "policy")
    public Map<String, PolicyConfig> rolePolicy;

    /**
     * If this is true and credentials are present then a user will always be authenticated
     * before the request progresses.
     * 
     * If this is false then an attempt will only be made to authenticate the user if a permission
     * check is performed or the current user is required for some other reason.
     */
    @ConfigItem(defaultValue = "true")
    public boolean proactive;
}
