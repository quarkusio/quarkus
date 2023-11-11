package io.quarkus.vertx.http.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Authentication mechanism information used for configuring HTTP auth instance for the deployment.
 */
@ConfigGroup
public class AuthRuntimeConfig {

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
     * The authentication realm
     */
    @ConfigItem
    public Optional<String> realm;

    /**
     * Form Auth config
     */
    @ConfigItem
    public FormAuthRuntimeConfig form;
}
