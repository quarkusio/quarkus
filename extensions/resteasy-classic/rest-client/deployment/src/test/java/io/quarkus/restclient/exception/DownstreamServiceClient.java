package io.quarkus.restclient.exception;

import javax.ws.rs.GET;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface DownstreamServiceClient {

    @GET
    String getData();

}
