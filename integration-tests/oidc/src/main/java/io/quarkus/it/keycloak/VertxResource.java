package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

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
    }

}
