package io.quarkus.rest.client.reactive;

import jakarta.ws.rs.GET;

public interface PlaylistService {

    @GET
    String get();
}
