package io.quarkus.rest.client.reactive.registerclientheaders;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface HeaderNoPassingClient {

    @GET
    @Path("/describe-request")
    RequestData call();

}
