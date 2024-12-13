package io.quarkus.vertx.http.runtime.management;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.vertx.http.runtime.PolicyConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.smallrye.config.WithName;

/**
 * Authentication for the management interface.
 */
public interface ManagementRuntimeAuthConfig {
    /**
     * The HTTP permissions
     */
    @WithName("permission")
    Map<String, PolicyMappingConfig> permissions();

    /**
     * The HTTP role based policies
     */
    @WithName("policy")
    Map<String, PolicyConfig> rolePolicy();

    /**
     * Map the `SecurityIdentity` roles to deployment specific roles and add the matching roles to `SecurityIdentity`.
     * <p>
     * For example, if `SecurityIdentity` has a `user` role and the endpoint is secured with a 'UserRole' role,
     * use this property to map the `user` role to the `UserRole` role, and have `SecurityIdentity` to have
     * both `user` and `UserRole` roles.
     */
    @ConfigDocMapKey("role-name")
    Map<String, List<String>> rolesMapping();
}
