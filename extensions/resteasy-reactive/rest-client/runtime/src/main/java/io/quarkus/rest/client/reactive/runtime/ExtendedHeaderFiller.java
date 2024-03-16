package io.quarkus.rest.client.reactive.runtime;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;

import io.quarkus.rest.client.reactive.HeaderFiller;

public interface ExtendedHeaderFiller extends HeaderFiller {

    void addHeaders(MultivaluedMap<String, String> headers, ResteasyReactiveClientRequestContext requestContext);

    default void addHeaders(MultivaluedMap<String, String> headers) {
        throw new IllegalStateException("should not be used");
    }
}
