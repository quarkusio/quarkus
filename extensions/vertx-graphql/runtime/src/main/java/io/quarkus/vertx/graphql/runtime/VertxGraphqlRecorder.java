package io.quarkus.vertx.graphql.runtime;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;

@Recorder
public class VertxGraphqlRecorder {
    public Handler<RoutingContext> handler(String path) {

        GraphiQLHandlerOptions options = new GraphiQLHandlerOptions();
        options.setEnabled(true);

        Handler<RoutingContext> handler = GraphiQLHandler.create(options);

        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (event.normalisedPath().length() == path.length()) {

                    event.response().setStatusCode(302);
                    event.response().headers().set(HttpHeaders.LOCATION, path + "/");
                    event.response().end();
                    return;
                }

                handler.handle(event);
            }
        };
    }
}
