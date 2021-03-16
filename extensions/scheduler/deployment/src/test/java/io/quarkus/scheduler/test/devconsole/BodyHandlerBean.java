package io.quarkus.scheduler.test.devconsole;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.configuration.ConfigInstantiator;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class BodyHandlerBean {

    void setup(@Observes Router router) {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        ConfigInstantiator.handleObject(httpConfiguration);
        Handler<RoutingContext> bodyHandler = new VertxHttpRecorder().createBodyHandler(httpConfiguration);
        router.route().order(Integer.MIN_VALUE + 1).handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext routingContext) {
                routingContext.request().resume();
                bodyHandler.handle(routingContext);
            }
        });
    }
}
