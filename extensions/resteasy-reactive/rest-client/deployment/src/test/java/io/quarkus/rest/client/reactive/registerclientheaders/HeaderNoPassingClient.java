package io.quarkus.rest.client.reactive.registerclientheaders;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.rest.client.reactive.TestJacksonBasicMessageBodyReader;

@RegisterRestClient
@RegisterProvider(TestJacksonBasicMessageBodyReader.class)
public interface HeaderNoPassingClient {

    @GET
    @Path("/describe-request")
    RequestData call();

}
