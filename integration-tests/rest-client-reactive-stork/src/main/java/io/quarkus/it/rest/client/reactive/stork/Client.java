package io.quarkus.it.rest.client.reactive.stork;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "hello")
public interface Client {
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    String echo(String name);
}
