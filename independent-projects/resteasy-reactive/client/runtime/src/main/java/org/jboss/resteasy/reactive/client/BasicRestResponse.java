package org.jboss.resteasy.reactive.client;

import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Provides access to HTTP response metadata (status code and headers)
 * when consuming a streaming response via {@link RestMultiResponse}.
 */
public interface BasicRestResponse {

    int status();

    MultivaluedMap<String, String> headers();
}
