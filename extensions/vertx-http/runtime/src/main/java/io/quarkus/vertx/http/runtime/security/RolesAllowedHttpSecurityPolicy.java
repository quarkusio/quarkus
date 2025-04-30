package io.quarkus.vertx.http.runtime.security;

import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * permission checker that handles role based permissions
 */
public class RolesAllowedHttpSecurityPolicy extends RolesMapping implements HttpSecurityPolicy {
    private static final String AUTHENTICATED = "**";
    private final String[] rolesAllowed;

    public RolesAllowedHttpSecurityPolicy(List<String> rolesAllowed, Map<String, Set<Permission>> roleToPermissions,
            Map<String, List<String>> roleToRoles) {
        super(roleToPermissions, roleToRoles);
        this.rolesAllowed = rolesAllowed.toArray(String[]::new);
    }

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.map(new Function<SecurityIdentity, CheckResult>() {
            @Override
            public CheckResult apply(SecurityIdentity securityIdentity) {
                if (grantPermissions || grantRoles) {
                    SecurityIdentity augmented = augmentIdentity(securityIdentity);
                    if (augmented != null) {
                        for (String i : rolesAllowed) {
                            if (augmented.hasRole(i) || (AUTHENTICATED.equals(i) && !augmented.isAnonymous())) {
                                return new CheckResult(true, augmented);
                            }
                        }
                        return new CheckResult(false, augmented);
                    }
                }
                for (String i : rolesAllowed) {
                    if (securityIdentity.hasRole(i) || (AUTHENTICATED.equals(i) && !securityIdentity.isAnonymous())) {
                        return CheckResult.PERMIT;
                    }
                }
                return CheckResult.DENY;
            }
        });
    }
}
