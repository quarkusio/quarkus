package io.quarkus.micrometer.runtime.export;

import java.util.function.Consumer;

import io.quarkus.micrometer.runtime.export.handlers.PrometheusHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.web.Route;

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
}
