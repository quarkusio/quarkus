package io.quarkus.vertx.http.runtime.management;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.vertx.http.runtime.PolicyConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;

/**
 * Authentication for the management interface.
 */
@ConfigGroup
public class ManagementRuntimeAuthConfig {

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
}
