package io.quarkus.resteasy.test.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomHttpSecurityPolicy implements HttpSecurityPolicy {

    @Inject
    ResourceInfo resourceInfo;

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        if ("CustomPolicyResource".equals(resourceInfo.getResourceClass().getSimpleName())
                && "isUserAdmin".equals(resourceInfo.getResourceMethod().getName())) {
            return identity.onItem().ifNotNull().transform(i -> {
                if (i.hasRole("user")) {
                    return new CheckResult(true, QuarkusSecurityIdentity.builder(i).addRole("admin").build());
                }
                return CheckResult.PERMIT;
            });
        }
        return Uni.createFrom().item(CheckResult.PERMIT);
    }

    @Override
    public String name() {
        return "custom";
    }
}
