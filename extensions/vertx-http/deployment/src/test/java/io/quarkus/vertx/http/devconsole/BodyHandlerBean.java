package io.quarkus.vertx.http.devconsole;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class BodyHandlerBean {
    @Inject
    HttpConfiguration httpConfiguration;
    @Inject
    HttpBuildTimeConfig httpBuildTimeConfig;
    @Inject
    ManagementInterfaceConfiguration managementInterfaceConfiguration;
    @Inject
    ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig;

    void setup(@Observes Router router) {
        Handler<RoutingContext> bodyHandler = new VertxHttpRecorder(httpBuildTimeConfig,
                managementInterfaceBuildTimeConfig,
                new RuntimeValue<>(httpConfiguration),
                new RuntimeValue<>(managementInterfaceConfiguration))
                .createBodyHandler();
        router.route().order(Integer.MIN_VALUE + 1).handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext routingContext) {
                routingContext.request().resume();
                bodyHandler.handle(routingContext);
            }
        });
    }
}
