package io.quarkus.vertx.http.testrunner.brokenonly;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class BrokenOnlyResource {

    public void route(@Observes Router router) {
        router.route("/hello/greeting/:name").handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                event.response().end("hello " + event.pathParam("name"));
            }
        });
        //setup(router);
    }

    void setup(Router router) {
        router.route("/hello").handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext routingContext) {
                routingContext.response().end("hello");
            }
        });
    }

}
