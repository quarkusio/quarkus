package io.quarkus.vertx.http.runtime.security;

import java.util.List;
import java.util.function.Function;

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
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.map(new Function<SecurityIdentity, CheckResult>() {
            @Override
            public CheckResult apply(SecurityIdentity securityIdentity) {
                for (String i : rolesAllowed) {
                    if (securityIdentity.hasRole(i)) {
                        return CheckResult.PERMIT;
                    }
                }
                return CheckResult.DENY;
            }
        });
    }
}
