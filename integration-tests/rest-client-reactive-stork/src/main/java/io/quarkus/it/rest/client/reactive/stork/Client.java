package io.quarkus.it.rest.client.reactive.stork;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "hello")
public interface Client {
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    String echo(String name);
}
