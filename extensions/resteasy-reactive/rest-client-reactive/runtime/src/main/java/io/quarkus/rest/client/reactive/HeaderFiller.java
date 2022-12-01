package io.quarkus.rest.client.reactive;

import javax.ws.rs.core.MultivaluedMap;

public interface HeaderFiller {
    void addHeaders(MultivaluedMap<String, String> headers);
}
