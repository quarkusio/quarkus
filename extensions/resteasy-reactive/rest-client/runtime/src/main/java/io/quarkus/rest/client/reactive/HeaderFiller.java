package io.quarkus.rest.client.reactive;

import jakarta.ws.rs.core.MultivaluedMap;

public interface HeaderFiller {
    void addHeaders(MultivaluedMap<String, String> headers);
}
