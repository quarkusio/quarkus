package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class PermitUserAuthorizationPolicy implements HttpSecurityPolicy {

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.map(i -> {
            if (!i.isAnonymous() && "user".equals(i.getPrincipal().getName())) {
                return CheckResult.PERMIT;
            }
            return CheckResult.DENY;
        });
    }

    @Override
    public String name() {
        return "permit-user";
    }
}
