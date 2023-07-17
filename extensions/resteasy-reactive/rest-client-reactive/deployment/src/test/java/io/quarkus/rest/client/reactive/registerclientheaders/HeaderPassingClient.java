package io.quarkus.rest.client.reactive.registerclientheaders;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.rest.client.reactive.TestJacksonBasicMessageBodyReader;

@RegisterRestClient
@RegisterClientHeaders
@RegisterProvider(TestJacksonBasicMessageBodyReader.class)
public interface HeaderPassingClient {

    @GET
    @Path("/describe-request")
    RequestData call();

}
