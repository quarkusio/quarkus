package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
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
     * Map the `SecurityIdentity` roles to deployment specific roles and add the matching roles to `SecurityIdentity`.
     * <p>
     * For example, if `SecurityIdentity` has a `user` role and the endpoint is secured with a 'UserRole' role,
     * use this property to map the `user` role to the `UserRole` role, and have `SecurityIdentity` to have
     * both `user` and `UserRole` roles.
     */
    @ConfigItem
    @ConfigDocMapKey("role-name")
    public Map<String, List<String>> rolesMapping;

    /**
     * Properties file containing the client certificate common name (CN) to role mappings.
     * Use it only if the mTLS authentication mechanism is enabled with either
     * `quarkus.http.ssl.client-auth=required` or `quarkus.http.ssl.client-auth=request`.
     * <p/>
     * Properties file is expected to have the `CN=role1,role,...,roleN` format and should be encoded using UTF-8.
     */
    @ConfigItem
    public Optional<Path> certificateRoleProperties;

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
