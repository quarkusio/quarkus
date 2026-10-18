package org.jboss.resteasy.reactive.client.api;

import java.util.Set;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

public interface ClientLogger {
    void setBodySize(int bodySize);

    /**
     * Sets the header names whose values should be masked during log output.
     *
     * @param maskedHeaders Contains lower case header names. immutable, not null.
     */
    default void setMaskedHeaders(Set<String> maskedHeaders) {
    }

    void logResponse(HttpClientResponse response, boolean redirect);

    void logRequest(HttpClientRequest request, Buffer body, boolean omitBody);
}
