package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class ViewerAugmentingPolicy implements HttpSecurityPolicy {

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.flatMap(i -> {
            if (!i.isAnonymous() && i.getPrincipal().getName().equals("viewer")) {
                var newIdentity = QuarkusSecurityIdentity.builder(i).addRole("admin").build();
                return Uni.createFrom().item(new CheckResult(true, newIdentity));
            }
            return CheckResult.permit();
        });
    }

    @Override
    public String name() {
        return "viewer-augmenting-policy";
    }
}
