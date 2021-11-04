package io.quarkus.vertx.graphql.runtime;

import java.util.function.Consumer;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;

@Recorder
public class VertxGraphqlRecorder {
    public Handler<RoutingContext> handler() {

        GraphiQLHandlerOptions options = new GraphiQLHandlerOptions();
        options.setEnabled(true);

        Handler<RoutingContext> handler = GraphiQLHandler.create(options);

        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (event.normalizedPath().length() == (event.currentRoute().getPath().length()
                        + (event.mountPoint() == null ? 0 : event.mountPoint().length() - 1))) {
                    event.response().setStatusCode(302);
                    event.response().headers().set(HttpHeaders.LOCATION, (event.mountPoint() == null ? "" : event.mountPoint())
                            + event.currentRoute().getPath().substring(1) + "/");
                    event.response().end();
                    return;
                }

                handler.handle(event);
            }
        };
    }

    public Consumer<Route> routeFunction(Handler<RoutingContext> bodyHandler) {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                route.handler(bodyHandler);
            }
        };
    }

}
