package io.quarkus.rest.client.reactive.runtime;

import jakarta.ws.rs.core.MultivaluedMap;

import io.quarkus.rest.client.reactive.HeaderFiller;

public class NoOpHeaderFiller implements HeaderFiller {
    @Override
    public void addHeaders(MultivaluedMap<String, String> headers) {
    }

    @SuppressWarnings("unused")
    public static final NoOpHeaderFiller INSTANCE = new NoOpHeaderFiller();
}
