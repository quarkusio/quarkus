package io.quarkus.vertx.http.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PermissionSetConfig {

    /**
     * A list of roles allowed to access this resource.
     *
     * The special role name '*' can be used to represent any authenticated user.
     *
     * If this is set the permit-all and deny-all must not be set
     */
    @ConfigItem
    public List<String> rolesAllowed;

    /**
     * If this is true any user (including unauthenticated users) can access these resources.
     *
     * If this is set then deny-all and roles-allowed must not be set.
     */
    @ConfigItem
    public boolean permitAll;

    /**
     * If this is true access to these resources are denied for all users
     *
     * If this is set then permit-all and roles-allowed must not be set.
     */
    @ConfigItem
    public boolean denyAll;

    /**
     * The methods that this permission set applies to. If this is not set then they apply to all methods.
     *
     * Note that if a request matches any path from any permission set, but does not match the constraint
     * due to the method not being listed then the request will be denied.
     *
     * Method specific permissions take precedence over matches that do not have any methods set.
     *
     * This means that for example if Quarkus is configured to allow GET and POST requests to /admin to
     * and no other permissions are configured PUT requests to /admin will be denied.
     *
     */
    @ConfigItem
    public List<String> methods;

    /**
     * The paths that this permission check applies to. If the path ends in /* then this is treated
     * as a path prefix, otherwise it is treated as an exact match.
     *
     * Matches are done on a length basis, so the most specific path match takes precedence.
     *
     * If multiple permission sets match the same path then explicit methods matches take precedence
     * over over matches without methods set, otherwise the most restrictive permissions are applied.
     *
     */
    @ConfigItem
    public List<String> paths;
}
