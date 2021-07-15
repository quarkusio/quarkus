package io.quarkus.vertx.http.devconsole;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class ConfigBean {

    @ConfigProperty(name = "message")
    String message;

    void route(@Observes Router router) {
        router.route("/msg").handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                event.response().end(message);
            }
        });

    }

}
