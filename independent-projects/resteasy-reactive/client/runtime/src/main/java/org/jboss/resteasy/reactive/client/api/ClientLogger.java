package org.jboss.resteasy.reactive.client.api;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

public interface ClientLogger {
    void setBodySize(int bodySize);

    void logResponse(HttpClientResponse response, boolean redirect);

    void logRequest(HttpClientRequest request, Buffer body, boolean omitBody);
}
