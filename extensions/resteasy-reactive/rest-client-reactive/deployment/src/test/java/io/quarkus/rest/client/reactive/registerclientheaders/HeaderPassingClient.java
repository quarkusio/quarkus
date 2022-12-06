package io.quarkus.rest.client.reactive.registerclientheaders;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@RegisterClientHeaders
public interface HeaderPassingClient {

    @GET
    @Path("/describe-request")
    RequestData call();

}
