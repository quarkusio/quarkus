package io.quarkus.vertx.http.runtime.logging;

public enum RequestLoggerFormat {

    LONG,
    DEFAULT,
    SHORT,
    TINY;

    public static RequestLoggerFormat parse(String s) {
        return RequestLoggerFormat.valueOf(s.toUpperCase());
    }

}
