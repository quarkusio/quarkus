package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class VertxResource {

    void setup(@Observes Router router) {
        router.route("/vertx").handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                event.request().bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer data) {
                        event.response().end(data);
                    }
                });
            }
        });
        router.route("/basic-only").handler(new Handler<RoutingContext>() {
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
        router.route("/bearer-only").handler(new Handler<RoutingContext>() {
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
