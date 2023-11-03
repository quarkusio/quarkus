package io.quarkus.rest.client.reactive.stork;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/helper")
public class PassThroughResource {

    @RestClient
    HelloClient client;

    @GET
    public String invokeClient() {
        HelloClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create("stork://hello-service/hello"))
                .build(HelloClient.class);
        return client.hello();
    }

    @Path("/cdi")
    @GET
    public String invokeCdiClient() {
        return client.hello();
    }
}
