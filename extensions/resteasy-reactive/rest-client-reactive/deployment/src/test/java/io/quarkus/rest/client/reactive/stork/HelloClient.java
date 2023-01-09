package io.quarkus.rest.client.reactive.stork;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "hello2")
public interface HelloClient {
    @GET
    String hello();
}
