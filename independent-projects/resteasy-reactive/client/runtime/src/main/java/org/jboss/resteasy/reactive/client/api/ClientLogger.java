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

    /**
     * Analogue of {@link #logResponse(HttpClientResponse, boolean)} for responses whose body
     * has already been consumed. Obtaining the body again from {@code response} yields an empty buffer.
     * <p>
     * The default implementation delegates to {@link #logResponse(HttpClientResponse, boolean)}
     * for backwards compatibility and therefore logs an empty body. Implementations that log
     * response bodies should override this method.
     */
    default void logResponse(HttpClientResponse response, Buffer body, boolean redirect) {
        logResponse(response, redirect);
    }

    void logRequest(HttpClientRequest request, Buffer body, boolean omitBody);
}
