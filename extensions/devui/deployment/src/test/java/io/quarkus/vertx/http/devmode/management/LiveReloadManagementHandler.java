package io.quarkus.vertx.http.devmode.management;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class LiveReloadManagementHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        LiveReloadManagementBean managementBean = Arc.container().instance(LiveReloadManagementBean.class).get();
        event.response().end(managementBean.string());
    }
}
