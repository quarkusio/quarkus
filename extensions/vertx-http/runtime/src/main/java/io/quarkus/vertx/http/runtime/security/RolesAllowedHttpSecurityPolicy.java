package io.quarkus.vertx.http.runtime.security;

import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * permission checker that handles role based permissions
 */
public class RolesAllowedHttpSecurityPolicy implements HttpSecurityPolicy {
    private List<String> rolesAllowed;

    public RolesAllowedHttpSecurityPolicy(List<String> rolesAllowed) {
        this.rolesAllowed = rolesAllowed;
    }

    public RolesAllowedHttpSecurityPolicy() {
    }

    public List<String> getRolesAllowed() {
        return rolesAllowed;
    }

    public RolesAllowedHttpSecurityPolicy setRolesAllowed(List<String> rolesAllowed) {
        this.rolesAllowed = rolesAllowed;
        return this;
    }

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, SecurityIdentity identity,
            AuthorizationRequestContext requestContext) {
        for (String i : rolesAllowed) {
            if (identity.hasRole(i)) {
                return Uni.createFrom().item(CheckResult.PERMIT);
            }
        }
        return Uni.createFrom().item(CheckResult.DENY);
    }
}
