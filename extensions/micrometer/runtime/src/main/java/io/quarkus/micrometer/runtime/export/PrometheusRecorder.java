package io.quarkus.micrometer.runtime.export;

import java.util.function.Consumer;

import io.quarkus.micrometer.runtime.export.handlers.PrometheusHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class PrometheusRecorder {
    PrometheusHandler handler;

    public PrometheusHandler getHandler() {
        if (handler == null) {
            handler = new PrometheusHandler();
        }

        return handler;
    }

    public Consumer<Route> route() {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                route.order(1).produces("text/plain");
            }
        };
    }

    public Consumer<Route> fallbackRoute() {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                route.order(4);
            }
        };
    }

    public Handler<RoutingContext> getFallbackHandler() {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext routingContext) {
                routingContext.response()
                        .setStatusCode(406)
                        .setStatusMessage(
                                "Micrometer prometheus endpoint does not support "
                                        + routingContext.request().getHeader(HttpHeaders.ACCEPT))
                        .end();
            }
        };
    }
}
