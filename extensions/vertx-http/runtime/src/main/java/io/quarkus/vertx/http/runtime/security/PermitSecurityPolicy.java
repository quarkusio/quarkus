package io.quarkus.vertx.http.runtime.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class PermitSecurityPolicy implements HttpSecurityPolicy {

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, SecurityIdentity identity,
            AuthorizationRequestContext requestContext) {
        return Uni.createFrom().item(CheckResult.PERMIT);
    }
}
