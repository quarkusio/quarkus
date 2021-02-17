package io.quarkus.resteasy.reactive.client.microprofile;

import javax.ws.rs.core.MultivaluedMap;

public interface HeaderFiller {
    void addHeaders(MultivaluedMap<String, String> headers);
}
