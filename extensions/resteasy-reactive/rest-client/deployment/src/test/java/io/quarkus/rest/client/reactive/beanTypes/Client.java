package io.quarkus.rest.client.reactive.beanTypes;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "hello2")
public interface Client extends Alpha {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String test();

}
