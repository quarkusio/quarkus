package io.quarkus.rest.client.reactive;

import javax.ws.rs.GET;

public interface PlaylistService {

    @GET
    String get();
}
