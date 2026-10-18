package io.quarkus.test.reload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class SomeBeanClient {

    @Inject
    SomeBean someBean;

    void route(@Observes Router router) {
        router.route("/test").handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                event.response().end(someBean.ping());
            }
        });

    }

}
