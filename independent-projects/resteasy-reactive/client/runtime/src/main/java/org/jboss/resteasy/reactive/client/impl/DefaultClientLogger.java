package org.jboss.resteasy.reactive.client.impl;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.api.ClientLogger;

public class DefaultClientLogger implements ClientLogger {
    private static final Logger log = Logger.getLogger(DefaultClientLogger.class);

    private int bodySize;

    @Override
    public void setBodySize(int bodySize) {
        this.bodySize = bodySize;
    }

    @Override
    public void logResponse(HttpClientResponse response, boolean redirect) {
        response.bodyHandler(body -> log.debugf("%s: %s %s, Status[%d %s], Headers[%s], Body:\n%s",
                redirect ? "Redirect" : "Response",
                response.request().getMethod(), response.request().absoluteURI(), response.statusCode(),
                response.statusMessage(), asString(response.headers()), bodyToString(body)));
    }

    @Override
    public void logRequest(HttpClientRequest request, Buffer body, boolean multipart) {
        if (multipart) {
            log.debugf("Request: %s %s Headers[%s], Body omitted, a multipart message",
                    request.getMethod(), request.absoluteURI(), asString(request.headers()));
        } else if (body == null || body.length() == 0) {
            log.debugf("Request: %s %s Headers[%s], Empty body",
                    request.getMethod(), request.absoluteURI(), asString(request.headers()));
        } else {
            log.debugf("Request: %s %s Headers[%s], Body:\n%s",
                    request.getMethod(), request.absoluteURI(), asString(request.headers()), bodyToString(body));
        }
    }

    private String bodyToString(Buffer body) {
        if (body == null) {
            return "";
        } else {
            String bodyAsString = body.toString();
            return bodyAsString.substring(0, Math.min(bodySize, bodyAsString.length()));
        }
    }

    private String asString(MultiMap headers) {
        return headers.entries().stream()
                .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                .collect(Collectors.joining(" "));
    }
}
