package io.quarkus.email.authentication.deployment.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
class TestPathHandler {

    public void setup(@Observes Router router) {
        router.route().order(2).handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                QuarkusHttpUser user = (QuarkusHttpUser) event.user();
                StringBuilder ret = new StringBuilder();
                if (user != null) {
                    ret.append(user.getSecurityIdentity().getPrincipal().getName());
                }
                ret.append(":");
                ret.append(event.normalizedPath());
                event.response().end(ret.toString());
            }
        });
    }
}
