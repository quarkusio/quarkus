package io.quarkus.vertx.http.runtime.logging;

import io.vertx.core.http.HttpVersion;

public class RequestLogMessageFormatter {

    public String format(RequestLogMessage requestLogMessage) {
        return requestLogMessage.getMethod() + " " + requestLogMessage.getPath() + " " + format(requestLogMessage.getVersion())
                + System.lineSeparator()
                + requestLogMessage.getHeaders().entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .reduce((h1, h2) -> String.join(System.lineSeparator(), h1, h2))
                        .orElse("");
    }

    private String format(HttpVersion version) {
        switch (version) {
            case HTTP_1_0:
                return "HTTP/1.0";
            case HTTP_1_1:
                return "HTTP/1.1";
            case HTTP_2:
                return "HTTP/2";
            default:
                return version.toString();
        }
    }

}
