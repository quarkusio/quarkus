package io.quarkus.micrometer.runtime.export;

import java.util.function.Consumer;

import io.quarkus.micrometer.runtime.export.handlers.JsonHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.web.Route;

@Recorder
public class JsonRecorder {
    JsonHandler handler;

    public JsonHandler getHandler() {
        if (handler == null) {
            handler = new JsonHandler();
        }

        return handler;
    }

    public Consumer<Route> route() {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                route.order(2).produces("application/json");
            }
        };
    }
}
