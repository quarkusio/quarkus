package io.quarkus.rest.client.reactive.registerclientheaders;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface HeaderNoPassingClient {

    @GET
    @Path("/describe-request")
    RequestData call();

}
