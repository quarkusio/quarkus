package io.quarkus.vertx.http.security;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomNamedHttpSecPolicy implements HttpSecurityPolicy {
    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        if (isRequestAuthorized(request)) {
            return Uni.createFrom().item(CheckResult.PERMIT);
        }
        return Uni.createFrom().item(CheckResult.DENY);
    }

    private static boolean isRequestAuthorized(RoutingContext request) {
        return request.request().headers().contains("hush-hush");
    }

    @Override
    public String name() {
        return "custom123";
    }
}
