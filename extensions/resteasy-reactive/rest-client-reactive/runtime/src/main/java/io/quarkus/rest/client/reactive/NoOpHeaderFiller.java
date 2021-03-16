package io.quarkus.rest.client.reactive;

import javax.ws.rs.core.MultivaluedMap;

public class NoOpHeaderFiller implements HeaderFiller {
    @Override
    public void addHeaders(MultivaluedMap<String, String> headers) {
    }

    @SuppressWarnings("unused")
    public static final NoOpHeaderFiller INSTANCE = new NoOpHeaderFiller();
}
