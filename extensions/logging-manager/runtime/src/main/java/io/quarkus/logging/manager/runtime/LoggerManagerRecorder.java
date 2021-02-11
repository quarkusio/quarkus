package io.quarkus.logging.manager.runtime;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.logstream.LogStreamWebSocket;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class LoggerManagerRecorder {

    public Handler<RoutingContext> uiHandler(String loggingManagerFinalDestination, String loggingManagerPath,
            LoggingManagerRuntimeConfig runtimeConfig) {

        if (runtimeConfig.enable) {
            return new LoggingManagerStaticHandler(loggingManagerFinalDestination, loggingManagerPath);
        } else {
            return new LoggingManagerNotFoundHandler();
        }
    }

    public Handler<RoutingContext> logStreamWebSocketHandler(LoggingManagerRuntimeConfig runtimeConfig) {
        if (runtimeConfig.enable) {
            return new LogStreamWebSocket();
        } else {
            return new LoggingManagerNotFoundHandler();
        }
    }
}
