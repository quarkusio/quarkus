package org.jboss.resteasy.reactive.client;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * A {@link Multi} that also provides non-blocking access to the HTTP response
 * metadata (status code, headers) via {@link #response()}.
 * <p>
 * Returned by REST Client methods that consume streaming responses (SSE, newline-delimited JSON,
 * or chunked) when response metadata is needed alongside the event stream.
 */
public interface RestMultiResponse<T> extends Multi<T> {

    /**
     * Returns a {@link Uni} that completes when the HTTP response is received,
     * before any stream items are emitted
     */
    Uni<BasicRestResponse> response();
}
