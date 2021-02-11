package io.quarkus.micrometer.runtime.export;

import java.util.function.Function;

import io.quarkus.micrometer.runtime.export.handlers.PrometheusHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

@Recorder
public class PrometheusRecorder {
    PrometheusHandler handler;

    public PrometheusHandler getHandler() {
        if (handler == null) {
            handler = new PrometheusHandler();
        }

        return handler;
    }

    public Function<Router, Route> route(String path) {
        return new Function<Router, Route>() {
            @Override
            public Route apply(Router router) {
                return router.route(path).order(1).produces("text/plain");
            }
        };
    }
}
