package io.quarkus.it.keycloak;

import jakarta.enterprise.event.Observes;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.vertx.http.ManagementInterface;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

public class ManagementInterfaceCustomRoute {

    void init(@Observes ManagementInterface mi, IdentityProviderManager ipm) {
        mi.router().get("/q/management-secured").handler(rc -> QuarkusHttpUser
                .getSecurityIdentity(rc, ipm)
                .map(i -> i.isAnonymous() ? "anonymous" : i.getPrincipal().getName())
                .subscribe().with(rc::end, err -> fail(rc)));
        mi.router().get("/q/management-public").handler(rc -> rc.end("this route is public"));
    }

    private static void fail(RoutingContext rc) {
        rc.fail(500,
                new IllegalStateException("This route must only be accessible by authenticated user with 'management' role"));
    }

}
