package io.quarkus.vertx.http.runtime.logging;

import java.util.Optional;

import io.vertx.ext.web.handler.LoggerFormat;

public class VertxLoggerFormatMapper {

    public Optional<LoggerFormat> map(RequestLoggerFormat requestLoggerFormat) {
        switch (requestLoggerFormat) {
            case DEFAULT:
                return Optional.of(LoggerFormat.DEFAULT);
            case SHORT:
                return Optional.of(LoggerFormat.SHORT);
            case TINY:
                return Optional.of(LoggerFormat.TINY);
            default:
                return Optional.empty();
        }
    }

}
