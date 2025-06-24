package io.quarkus.vertx.http.runtime.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class PermitSecurityPolicy implements HttpSecurityPolicy {

    public static final String NAME = "permit";

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return CheckResult.permit();
    }
}
