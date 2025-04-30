package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

public interface PolicyConfig {
    /**
     * The roles that are allowed to access resources protected by this policy.
     * By default, access is allowed to any authenticated user.
     */
    @WithDefault("**")
    List<@WithConverter(TrimmedStringConverter.class) String> rolesAllowed();

    /**
     * Add roles granted to the `SecurityIdentity` based on the roles that the `SecurityIdentity` already have.
     * For example, the Quarkus OIDC extension can map roles from the verified JWT access token, and you may want
     * to remap them to a deployment specific roles.
     */
    @ConfigDocMapKey("role-name")
    Map<String, List<String>> roles();

    /**
     * Permissions granted to the `SecurityIdentity` if this policy is applied successfully
     * (the policy allows request to proceed) and the authenticated request has required role.
     * For example, you can map permission `perm1` with actions `action1` and `action2` to role `admin` by setting
     * `quarkus.http.auth.policy.role-policy1.permissions.admin=perm1:action1,perm1:action2` configuration property.
     * Granted permissions are used for authorization with the `@PermissionsAllowed` annotation.
     */
    @ConfigDocMapKey("role-name")
    Map<String, List<String>> permissions();

    /**
     * Permissions granted by this policy will be created with a `java.security.Permission` implementation
     * specified by this configuration property. The permission class must declare exactly one constructor
     * that accepts permission name (`String`) or permission name and actions (`String`, `String[]`).
     * Permission class must be registered for reflection if you run your application in a native mode.
     */
    @WithDefault("io.quarkus.security.StringPermission")
    String permissionClass();
}
