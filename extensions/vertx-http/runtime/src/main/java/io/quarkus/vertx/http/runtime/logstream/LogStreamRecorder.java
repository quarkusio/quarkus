package io.quarkus.vertx.http.runtime.logstream;

import java.util.Optional;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class LogStreamRecorder {

    public RuntimeValue<Optional<HistoryHandler>> handler(int size) {
        return new RuntimeValue<>(Optional.of(new HistoryHandler(size)));
    }

    public Handler<RoutingContext> websocketHandler(RuntimeValue<Optional<HistoryHandler>> historyHandler) {
        //we need to make sure this is created after logging
        //is initialized, as it sets up a handler
        return new LogStreamWebSocket(historyHandler.getValue().get());
    }
}
