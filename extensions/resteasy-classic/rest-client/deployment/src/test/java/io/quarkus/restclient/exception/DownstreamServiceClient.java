package io.quarkus.restclient.exception;

import jakarta.ws.rs.GET;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface DownstreamServiceClient {

    @GET
    String getData();

}
