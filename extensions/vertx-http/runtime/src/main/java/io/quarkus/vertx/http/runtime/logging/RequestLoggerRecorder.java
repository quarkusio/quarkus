package io.quarkus.vertx.http.runtime.logging;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerHandler;

@Recorder
public class RequestLoggerRecorder {

    private static final RequestLogMessageFormatter REQUEST_LOG_MESSAGE_FORMATTER = new RequestLogMessageFormatter();

    public static final VertxLoggerFormatMapper VERTX_LOGGER_FORMAT_MAPPER = new VertxLoggerFormatMapper();

    public Handler<RoutingContext> handler(RequestLoggerFormat format) {
        return VERTX_LOGGER_FORMAT_MAPPER.map(format).<Handler<RoutingContext>> map(LoggerHandler::create)
                .orElseGet(() -> new CustomFormatRequestLoggingHandler(REQUEST_LOG_MESSAGE_FORMATTER));
    }

}
